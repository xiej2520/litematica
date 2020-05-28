package fi.dy.masa.litematica.schematic;

import java.io.File;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStatePalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerSparse;
import fi.dy.masa.litematica.schematic.container.VanillaStructurePalette;
import fi.dy.masa.litematica.schematic.conversion.ConversionUtils;
import fi.dy.masa.litematica.schematic.conversion.IListTagDataConverter;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataConversionManager;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataPiece;
import fi.dy.masa.litematica.schematic.conversion.converter.BlockEntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.EntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.EntityDataConverterBase;
import fi.dy.masa.litematica.schematic.conversion.converter.InventoryDataConverter;
import fi.dy.masa.litematica.schematic.util.SchematicDataUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class VanillaStructure extends SingleRegionSchematic
{
    public static final String FILE_NAME_EXTENSION = ".nbt";

    VanillaStructure(File file)
    {
        super(file);
    }

    @Override
    public SchematicType<?> getType()
    {
        return SchematicType.VANILLA;
    }

    public static boolean isValidSchematic(NBTTagCompound tag)
    {
        if (tag.hasKey("blocks", Constants.NBT.TAG_LIST) &&
            tag.hasKey("palette", Constants.NBT.TAG_LIST) &&
            tag.hasKey("size", Constants.NBT.TAG_LIST))
        {
            return isSizeValid(readSizeFromTagImpl(tag));
        }

        return false;
    }

    @Override
    @Nullable
    protected Vec3i readSizeFromTag(NBTTagCompound tag)
    {
        return readSizeFromTagImpl(tag);
    }

    @Nullable
    private static Vec3i readSizeFromTagImpl(NBTTagCompound tag)
    {
        return NBTUtils.readBlockPosFromListTag(tag, "size");
    }

    @Override
    protected boolean setDataVersionFromTag(NBTTagCompound tag)
    {
        int dataVersion;

        if (tag.hasKey("DataVersion", Constants.NBT.TAG_INT))
        {
            dataVersion = tag.getInteger("DataVersion");
        }
        else
        {
            dataVersion = MinecraftVersion.MC_1_12_X.getMaxDataVersion();
        }

        this.setCurrentDataVersionWithFallback(dataVersion);

        return true;
    }

    @Override
    protected void createEmptyContainer(Vec3i size)
    {
        this.blockContainer = new LitematicaBlockStateContainerSparse(size);
    }

    @Override
    protected Class<? extends ILitematicaBlockStateContainer> getContainerClass()
    {
        return LitematicaBlockStateContainerSparse.class;
    }

    @Override
    protected boolean readBlocksFromTag(NBTTagCompound tag, boolean needsVersionConversion)
    {
        if (tag.hasKey("palette", Constants.NBT.TAG_LIST) &&
            tag.hasKey("blocks", Constants.NBT.TAG_LIST) &&
            isSizeValid(this.getSize()))
        {
            final MinecraftVersion versionFrom = this.getCurrentSchematicDataVersion().getMinecraftVersion();
            final MinecraftVersion versionTo = CURRENT_MINECRAFT_VERSION;
            final BlockEntityDataConverter beConverter = needsVersionConversion ? SchematicDataConversionManager.INSTANCE.getBlockEntityDataConverter(versionFrom, versionTo) : null;
            final InventoryDataConverter invConverter = needsVersionConversion ? SchematicDataConversionManager.INSTANCE.getInventoryDataConverter(versionFrom, versionTo) : null;

            NBTTagList paletteTag = tag.getTagList("palette", Constants.NBT.TAG_COMPOUND);
            LitematicaBlockStateContainerSparse container = (LitematicaBlockStateContainerSparse) this.blockContainer;
            ILitematicaBlockStatePalette palette = container.getPalette();

            if (needsVersionConversion)
            {
                paletteTag = this.convertBlockStatePaletteToCurrentGameVersion(paletteTag);
            }

            if (SchematicDataUtils.readPaletteFromLitematicaFormatTag(paletteTag, palette) == false)
            {
                InfoUtils.printErrorMessage("litematica.message.error.schematic_read.vanilla.failed_to_read_palette");
                return false;
            }

            if (tag.hasKey("author", Constants.NBT.TAG_STRING))
            {
                this.getMetadata().setAuthor(tag.getString("author"));
            }

            NBTTagList blockList = tag.getTagList("blocks", Constants.NBT.TAG_COMPOUND);
            ImmutableMap.Builder<BlockPos, NBTTagCompound> builderBlockEntities = ImmutableMap.builder();
            final int count = blockList.tagCount();

            for (int i = 0; i < count; ++i)
            {
                NBTTagCompound blockTag = blockList.getCompoundTagAt(i);
                BlockPos pos = NBTUtils.readBlockPosFromListTag(blockTag, "pos");

                if (pos == null)
                {
                    InfoUtils.printErrorMessage("litematica.message.error.schematic_read.vanilla.failed_to_read_block_pos");
                    return false;
                }

                int id = blockTag.getInteger("state");
                IBlockState state = palette.getBlockState(id);

                if (state == null)
                {
                    state = Blocks.AIR.getDefaultState();
                }

                container.setBlockState(pos.getX(), pos.getY(), pos.getZ(), state);

                if (blockTag.hasKey("nbt", Constants.NBT.TAG_COMPOUND))
                {
                    NBTTagCompound nbt = blockTag.getCompoundTag("nbt").copy();

                    if (needsVersionConversion)
                    {
                        if (beConverter != null)
                        {
                            beConverter.convertName(nbt, "id");
                        }

                        if (invConverter != null)
                        {
                            invConverter.convertAnyInventoryContents(nbt.getString("id"), nbt);
                        }
                    }

                    builderBlockEntities.put(pos, nbt);
                }
            }

            this.blockEntities = builderBlockEntities.build();

            return true;
        }

        return false;
    }

    @Override
    protected ImmutableMap<BlockPos, NBTTagCompound> readBlockEntitiesFromTag(NBTTagCompound tag, @Nullable BlockEntityDataConverter beConverter, @Nullable InventoryDataConverter invConverter)
    {
        // The data is read together with the blocks themselves, so just return the existing map
        return this.blockEntities;
    }

    @Override
    protected ImmutableList<EntityInfo> readEntitiesFromTag(NBTTagCompound tag, @Nullable EntityDataConverter entityConverter, @Nullable InventoryDataConverter invConverter)
    {
        ImmutableList.Builder<EntityInfo> builder = ImmutableList.builder();
        NBTTagList tagList = tag.getTagList("entities", Constants.NBT.TAG_COMPOUND);
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound entityData = tagList.getCompoundTagAt(i);
            Vec3d pos = NBTUtils.readVec3dFromListTag(entityData, "pos");

            if (pos != null && entityData.hasKey("nbt", Constants.NBT.TAG_COMPOUND))
            {
                NBTTagCompound nbt = entityData.getCompoundTag("nbt").copy();
                ConversionUtils.convertEntityTag(nbt, "id", entityConverter, invConverter);
                builder.add(new EntityInfo(pos, nbt));
            }
        }

        return builder.build();
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = this.toTagBase();

        tag.setInteger("DataVersion", this.requestedOutputMinecraftVersion.getMaxDataVersion());

        return tag;
    }

    @Override
    protected void onWriteToTag(NBTTagCompound tagOut, @Nullable NBTTagCompound cachedTag)
    {
        // The block data has been modified, overwrite the cached values with the current values
        if (cachedTag == null)
        {
            NBTUtils.writeBlockPosToListTag(this.getSize(), tagOut, "size");
        }
    }

    @Override
    protected void writeMetadataToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag)
    {
        NBTTagCompound metaTag = this.getMetadataTagForWriting(cachedTag);
        String author = this.getMetadata().getAuthor();

        if (author.isEmpty() == false && metaTag.hasKey("author", Constants.NBT.TAG_STRING) == false)
        {
            tag.setString("author", author);
        }

        tag.setTag("Metadata", metaTag);
    }

    @Override
    protected void writeBlocksToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag, boolean needsConversion)
    {
        final MinecraftVersion versionFrom = this.getCurrentSchematicDataVersion().getMinecraftVersion();
        final MinecraftVersion versionTo = this.requestedOutputMinecraftVersion;
        // Dummy resize handler, the hash map palette doesn't need to be re-created
        final ILitematicaBlockStatePalette palette = new VanillaStructurePalette();
        final BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();
        final BlockEntityDataConverter beConverter = needsConversion ? SchematicDataConversionManager.INSTANCE.getBlockEntityDataConverter(versionFrom, versionTo) : null;
        final InventoryDataConverter invConverter = needsConversion ? SchematicDataConversionManager.INSTANCE.getInventoryDataConverter(versionFrom, versionTo) : null;
        NBTTagList blockList = new NBTTagList();
        NBTTagList paletteList;

        if (cachedTag != null &&
            this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCKS) &&
            this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCK_ENTITIES))
        {
            blockList = cachedTag.getTagList("blocks", Constants.NBT.TAG_COMPOUND);
            paletteList = cachedTag.getTagList("palette", Constants.NBT.TAG_COMPOUND);
        }
        else if (this.blockContainer instanceof LitematicaBlockStateContainerSparse)
        {
            LitematicaBlockStateContainerSparse container = (LitematicaBlockStateContainerSparse) this.blockContainer;
            Long2ObjectOpenHashMap<IBlockState> blockMap = container.getBlockMap();
            final NBTTagList blockListTmp = blockList;

            blockMap.forEach((posLong, state) -> {
                long pos = posLong.longValue();
                posMutable.setPos((int) (pos & 0xFFFF), (int) ((pos >>> 32) & 0xFFFF), (int) ((pos >> 16) & 0xFFFF));
                this.writeBlockToList(posMutable, palette.idFor(state), blockListTmp, beConverter, invConverter);
            });

            paletteList = SchematicDataUtils.writePaletteToLitematicaFormatTag(palette);
        }
        else
        {
            ILitematicaBlockStateContainer container = this.blockContainer;
            Vec3i size = container.getSize();
            final int sizeX = size.getX();
            final int sizeY = size.getY();
            final int sizeZ = size.getZ();
            long volume = PositionUtils.getAreaVolume(size);
            IBlockState ignore = volume < 100000 ? null : Blocks.AIR.getDefaultState();

            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        IBlockState state = container.getBlockState(x, y, z);

                        if (state != ignore)
                        {
                            posMutable.setPos(x, y, z);
                            this.writeBlockToList(posMutable, palette.idFor(state), blockList, beConverter, invConverter);
                        }
                    }
                }
            }

            paletteList = SchematicDataUtils.writePaletteToLitematicaFormatTag(palette);
        }

        if (needsConversion)
        {
            paletteList = ConversionUtils.convertBlockStatePalette(paletteList, this.getCurrentSchematicDataVersion(), versionTo);
        }

        tag.setTag("palette", paletteList);
        tag.setTag("blocks", blockList);
    }

    private void writeBlockToList(BlockPos.MutableBlockPos posMutable, int id, NBTTagList blockList,
                                  @Nullable BlockEntityDataConverter beConverter, @Nullable InventoryDataConverter invConverter)
    {
        NBTTagCompound blockTag = new NBTTagCompound();

        NBTUtils.writeBlockPosToListTag(posMutable, blockTag, "pos");
        blockTag.setInteger("state", id);

        NBTTagCompound beTag = this.blockEntities.get(posMutable);

        if (beTag != null)
        {
            beTag = beTag.copy();

            if (beConverter != null)
            {
                beConverter.convertName(beTag, "id");
            }

            if (invConverter != null)
            {
                invConverter.convertAnyInventoryContents(beTag.getString("id"), beTag);
            }

            blockTag.setTag("nbt", beTag);
        }

        blockList.appendTag(blockTag);
    }

    @Override
    protected void writeBlockEntitiesToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag, @Nullable IListTagDataConverter converter)
    {
        // NO-OP because the BlockEntity data is stored together with the block data in the vanilla format
    }

    @Override
    protected void writeEntitiesToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag, @Nullable IListTagDataConverter converter)
    {
        NBTTagList tagList = new NBTTagList();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (EntityInfo info : this.entities)
        {
            NBTTagCompound entityData = new NBTTagCompound();
            posMutable.setPos(info.pos.x, info.pos.y, info.pos.z);

            NBTUtils.writeVec3dToListTag(info.pos, entityData, "pos");
            NBTUtils.writeBlockPosToListTag(posMutable, entityData, "blockPos");

            NBTTagCompound entityTag = info.nbt.copy();

            EntityDataConverterBase entityConverter = ((ConversionUtils.EntityListDataConverter) converter).getEntityDataConverter();
            InventoryDataConverter invConverter = ((ConversionUtils.EntityListDataConverter) converter).getInventoryDataConverter();

            ConversionUtils.convertEntityTag(entityTag, "id", entityConverter, invConverter);

            entityTag.removeTag("Pos");
            entityData.setTag("nbt", entityTag);

            tagList.appendTag(entityData);
        }

        tag.setTag("entities", tagList);
    }
}
