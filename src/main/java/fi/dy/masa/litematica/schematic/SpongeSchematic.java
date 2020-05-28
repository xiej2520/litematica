package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStatePalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.schematic.conversion.ConversionUtils;
import fi.dy.masa.litematica.schematic.conversion.IListTagDataConverter;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataPiece;
import fi.dy.masa.litematica.schematic.conversion.converter.BlockEntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.EntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.InventoryDataConverter;
import fi.dy.masa.litematica.schematic.util.SchematicDataUtils;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;

public class SpongeSchematic extends SingleRegionSchematic
{
    public static final String FILE_NAME_EXTENSION = ".schem";

    protected int version = 2;

    SpongeSchematic(File file)
    {
        super(file);
    }

    @Override
    public SchematicType<?> getType()
    {
        return SchematicType.SPONGE;
    }

    public static boolean isValidSchematic(NBTTagCompound tag)
    {
        if (tag.hasKey("Width", Constants.NBT.TAG_ANY_NUMERIC) &&
            tag.hasKey("Height", Constants.NBT.TAG_ANY_NUMERIC) &&
            tag.hasKey("Length", Constants.NBT.TAG_ANY_NUMERIC) &&
            tag.hasKey("Version", Constants.NBT.TAG_INT) &&
            tag.hasKey("Palette", Constants.NBT.TAG_COMPOUND) &&
            tag.hasKey("BlockData", Constants.NBT.TAG_BYTE_ARRAY))
        {
            return isSizeValid(readSizeFromTagImpl(tag));
        }

        return false;
    }

    @Override
    protected boolean setDataVersionFromTag(NBTTagCompound tag)
    {
        this.version = tag.getInteger("Version");

        int dataVersion = MinecraftVersion.MC_1_13_X.getMaxDataVersion();

        if (this.version == 1)
        {
            dataVersion = MinecraftVersion.MC_1_12_X.getMaxDataVersion();;
        }
        else if (tag.hasKey("DataVersion", Constants.NBT.TAG_INT))
        {
            dataVersion = tag.getInteger("DataVersion");
        }

        this.setCurrentDataVersionWithFallback(dataVersion);

        return true;
    }

    @Override
    protected Vec3i readSizeFromTag(NBTTagCompound tag)
    {
        return readSizeFromTagImpl(tag);
    }

    private static Vec3i readSizeFromTagImpl(NBTTagCompound tag)
    {
        return new Vec3i(tag.getInteger("Width"), tag.getInteger("Height"), tag.getInteger("Length"));
    }

    @Override
    protected void readMetadataFromTag(NBTTagCompound tag)
    {
        super.readMetadataFromTag(tag);

        if (tag.hasKey("Metadata", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound metaTag = tag.getCompoundTag("Metadata");

            if (metaTag.hasKey("Date", Constants.NBT.TAG_LONG) &&
                this.getMetadata().getTimeCreated() <= 0)
            {
                long time = metaTag.getLong("Date");
                this.getMetadata().setTimeCreated(time);
                this.getMetadata().setTimeModified(time);
            }
        }
    }

    private boolean readPaletteFromTag(NBTTagCompound tag, ILitematicaBlockStatePalette palette, boolean needsVersionConversion)
    {
        NBTTagList paletteTag = SchematicDataUtils.convertSpongePaletteTagToLitematicaPalette(tag);

        if (needsVersionConversion)
        {
            paletteTag = this.convertBlockStatePaletteToCurrentGameVersion(paletteTag);
        }

        return SchematicDataUtils.readPaletteFromLitematicaFormatTag(paletteTag, palette);
    }

    @Override
    protected boolean readBlocksFromTag(NBTTagCompound tag, boolean needsVersionConversion)
    {
        if (tag.hasKey("Palette", Constants.NBT.TAG_COMPOUND) &&
            tag.hasKey("BlockData", Constants.NBT.TAG_BYTE_ARRAY) &&
            isSizeValid(this.getSize()))
        {
            NBTTagCompound paletteTag = tag.getCompoundTag("Palette");
            byte[] blockData = tag.getByteArray("BlockData");
            int paletteSize = paletteTag.getKeySet().size();

            this.blockContainer = LitematicaBlockStateContainerFull.createContainer(paletteSize, blockData, this.getSize());

            if (this.blockContainer == null)
            {
                InfoUtils.printErrorMessage("litematica.message.error.schematic_read.sponge.failed_to_read_blocks");
                return false;
            }

            return this.readPaletteFromTag(paletteTag, this.blockContainer.getPalette(), needsVersionConversion);
        }

        return false;
    }

    @Override
    protected ImmutableMap<BlockPos, NBTTagCompound> readBlockEntitiesFromTag(NBTTagCompound tag, @Nullable BlockEntityDataConverter beConverter, @Nullable InventoryDataConverter invConverter)
    {
        ImmutableMap.Builder<BlockPos, NBTTagCompound> builder = ImmutableMap.builder();

        String tagName = this.version == 1 ? "TileEntities" : "BlockEntities";
        NBTTagList tagList = tag.getTagList(tagName, Constants.NBT.TAG_COMPOUND);
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound beTag = tagList.getCompoundTagAt(i);
            BlockPos pos = NBTUtils.readBlockPosFromArrayTag(beTag, "Pos");

            if (pos != null && beTag.isEmpty() == false)
            {
                beTag = beTag.copy();
                beTag.setString("id", beTag.getString("Id"));

                ConversionUtils.convertEntityTag(beTag, "id", beConverter, invConverter);

                // Remove the Sponge tags from the data that is kept in memory
                beTag.removeTag("Id");
                beTag.removeTag("Pos");

                if (this.version == 1)
                {
                    beTag.removeTag("ContentVersion");
                }

                builder.put(pos, beTag);
            }
        }

        return builder.build();
    }

    @Override
    protected ImmutableList<EntityInfo> readEntitiesFromTag(NBTTagCompound tag, @Nullable EntityDataConverter entityConverter, @Nullable InventoryDataConverter invConverter)
    {
        ImmutableList.Builder<EntityInfo> builder = ImmutableList.builder();
        NBTTagList tagList = tag.getTagList("Entities", Constants.NBT.TAG_COMPOUND);
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound entityData = tagList.getCompoundTagAt(i);
            Vec3d pos = NBTUtils.readVec3dFromListTag(entityData);

            if (pos != null && entityData.isEmpty() == false)
            {
                entityData = entityData.copy();
                entityData.setString("id", entityData.getString("Id"));

                ConversionUtils.convertEntityTag(entityData, "id", entityConverter, invConverter);

                // Remove the Sponge tags from the data that is kept in memory
                entityData.removeTag("Id");

                builder.add(new EntityInfo(pos, entityData));
            }
        }

        return builder.build();
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = this.toTagBase();

        tag.setInteger("Version", this.version);
        tag.setInteger("DataVersion", this.requestedOutputMinecraftVersion.getMaxDataVersion());

        return tag;
    }

    @Override
    protected void onWriteToTag(NBTTagCompound tagOut, @Nullable NBTTagCompound cachedTag)
    {
        // The block data has been modified, overwrite the cached values with the current values
        if (cachedTag == null)
        {
            tagOut.setInteger("PaletteMax", this.blockContainer.getPalette().getPaletteSize() - 1);
            tagOut.setShort("Width", (short) this.getSize().getX());
            tagOut.setShort("Height", (short) this.getSize().getY());
            tagOut.setShort("Length", (short) this.getSize().getZ());
        }
    }

    @Override
    protected void writeMetadataToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag)
    {
        NBTTagCompound metaTag = this.getMetadataTagForWriting(cachedTag);

        if (this.getMetadata().getTimeCreated() > 0 && metaTag.hasKey("Date", Constants.NBT.TAG_LONG) == false)
        {
            metaTag.setLong("Date", this.getMetadata().getTimeCreated());
        }

        tag.setTag("Metadata", metaTag);
    }

    @Override
    protected void writeBlocksToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag, boolean needsConversion)
    {
        NBTTagCompound paletteCompound = null;
        NBTTagList paletteList = null;
        byte[] blockData = null;

        if (cachedTag != null && this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCKS))
        {
            blockData = cachedTag.getByteArray("BlockData");
            paletteCompound = cachedTag.getCompoundTag("Palette");
        }
        else
        {
            LitematicaBlockStateContainerFull blockContainer = (LitematicaBlockStateContainerFull) this.blockContainer;

            if (blockContainer != null)
            {
                blockData = blockContainer.getBackingArrayAsByteArray();
                paletteList = SchematicDataUtils.writePaletteToLitematicaFormatTag(blockContainer.getPalette());
            }
        }

        if (needsConversion)
        {
            // Convert to the Litematica palette format for the conversion methods
            if (paletteCompound != null)
            {
                paletteList = SchematicDataUtils.convertSpongePaletteTagToLitematicaPalette(paletteCompound);
            }

            if (paletteList != null)
            {
                paletteList = ConversionUtils.convertBlockStatePalette(paletteList, this.getCurrentSchematicDataVersion(), this.requestedOutputMinecraftVersion);
            }
        }

        // Convert (back) to the Sponge palette format
        if (paletteList != null)
        {
            paletteCompound = SchematicDataUtils.convertLitematicaPaletteToSpongePalette(paletteList);
        }

        if (blockData != null && paletteCompound != null)
        {
            tag.setTag("Palette", paletteCompound);
            tag.setByteArray("BlockData", blockData);
        }
    }

    @Override
    protected void writeBlockEntitiesToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag, @Nullable IListTagDataConverter converter)
    {
        String tagName = this.version == 1 ? "TileEntities" : "BlockEntities";
        SchematicDataUtils.writeListDataToTag(() -> this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCK_ENTITIES),
                                              tag, cachedTag, tagName, "Id", converter,
                                              this::writeBlockEntitiesToListTag);
    }

    @Override
    protected void writeEntitiesToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag, @Nullable IListTagDataConverter converter)
    {
        SchematicDataUtils.writeListDataToTag(() -> this.canSaveCachedDataDirectly(SchematicDataPiece.ENTITIES),
                                              tag, cachedTag, "Entities", "Id", converter,
                                              this::writeEntitiesToListTag);
    }

    private NBTTagList writeBlockEntitiesToListTag()
    {
        NBTTagList tagList = new NBTTagList();

        for (Map.Entry<BlockPos, NBTTagCompound> entry : this.blockEntities.entrySet())
        {
            NBTTagCompound beTag = entry.getValue().copy();
            NBTUtils.writeBlockPosToArrayTag(entry.getKey(), beTag, "Pos");

            // Add the Sponge tag and remove the vanilla/Litematica tag
            beTag.setString("Id", beTag.getString("id"));
            beTag.removeTag("id");

            if (this.version == 1)
            {
                beTag.setInteger("ContentVersion", 1);
            }

            tagList.appendTag(beTag);
        }

        return tagList;
    }

    private NBTTagList writeEntitiesToListTag()
    {
        NBTTagList listTag = new NBTTagList();

        for (EntityInfo info : this.entities)
        {
            NBTTagCompound entityData = info.nbt.copy();
            NBTUtils.writeVec3dToListTag(info.pos, entityData);

            // Add the Sponge tag and remove the vanilla/Litematica tag
            entityData.setString("Id", entityData.getString("id"));
            entityData.removeTag("id");

            if (this.version == 1)
            {
                entityData.setInteger("ContentVersion", 1);
            }

            listTag.appendTag(entityData);
        }

        return listTag;
    }
}
