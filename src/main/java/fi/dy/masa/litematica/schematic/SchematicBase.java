package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.mixin.IMixinDataFixer;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStatePalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.schematic.conversion.BlockEntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.BlockStateConverter;
import fi.dy.masa.litematica.schematic.conversion.BlockTickDataConverter;
import fi.dy.masa.litematica.schematic.conversion.EntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.EntityDataConverterBase;
import fi.dy.masa.litematica.schematic.conversion.InventoryDataConverter;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion.VersionClassification;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataConversionManager;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataVersion;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;

public abstract class SchematicBase implements ISchematic
{
    public static final int CURRENT_MINECRAFT_DATA_VERSION = ((IMixinDataFixer) Minecraft.getMinecraft().getDataFixer()).getVersion();
    public static final MinecraftVersion CURRENT_MINECRAFT_VERSION = MinecraftVersion.MC_1_12_X;

    public static final SchematicDataVersion CURRENT_SCHEMATIC_DATA_VERSION = SchematicDataVersion.getVersionFor(CURRENT_MINECRAFT_DATA_VERSION);

    protected final SchematicMetadata metadata = new SchematicMetadata();
    @Nullable
    protected final File schematicFile;

    private final EnumSet<SchematicDataPiece> dirtyData = EnumSet.noneOf(SchematicDataPiece.class);
    @Nullable
    private NBTTagCompound cachedNbtDataFromFile;
    private boolean shouldLoadFromCachedData;
    /** This is the data version that the cached NBT data read from the file is in */
    private SchematicDataVersion dataVersionFromFile = CURRENT_SCHEMATIC_DATA_VERSION;

    /** This is the schematic data version that the data should be written to file as */
    protected MinecraftVersion requestedOutputMinecraftVersion = CURRENT_MINECRAFT_VERSION;

    protected long totalBlocksReadFromWorld;

    public SchematicBase(@Nullable File file)
    {
        this.schematicFile = file;
    }

    @Override
    @Nullable
    public File getFile()
    {
        return this.schematicFile;
    }

    @Override
    public SchematicMetadata getMetadata()
    {
        return this.metadata;
    }

    @Override
    public void clear()
    {
        this.cachedNbtDataFromFile = null;
        this.shouldLoadFromCachedData = false;
        this.dataVersionFromFile = CURRENT_SCHEMATIC_DATA_VERSION;
        this.requestedOutputMinecraftVersion = CURRENT_MINECRAFT_VERSION;
    }

    @Override
    public SchematicDataVersion getCurrentSchematicDataVersion()
    {
        return this.dataVersionFromFile;
    }

    @Override
    public void setOutputMinecraftVersion(MinecraftVersion version)
    {
        this.requestedOutputMinecraftVersion = version;
    }

    protected void setCurrentDataVersionWithFallback(int dataVersion)
    {
        this.dataVersionFromFile = SchematicDataVersion.getVersionFor(dataVersion);
        boolean usedFallback = false;

        if (this.dataVersionFromFile == null)
        {
            MinecraftVersion fallback = CURRENT_MINECRAFT_VERSION; // this init version should never actually be used
            VersionClassification classification = MinecraftVersion.getVersionClassification(dataVersion);
            usedFallback = true;

            if (classification == VersionClassification.OLD)
            {
                fallback = MinecraftVersion.getOldestKnownVersion();

                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 8000, "litematica.error.schematic_conversion.unknown_data_version.old",
                        String.valueOf(dataVersion), fallback.getMcVersionDisplayName(), String.valueOf(fallback.getMaxDataVersion()));
            }
            else if (classification == VersionClassification.FUTURE)
            {
                fallback = MinecraftVersion.getLatestKnownVersion();

                String strLastKnown = fallback.getMcVersionDisplayName();
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 8000, "litematica.error.schematic_conversion.unknown_data_version.future",
                        String.valueOf(dataVersion), strLastKnown, fallback.getMcVersionDisplayName(), String.valueOf(fallback.getMaxDataVersion()));
            }

            this.dataVersionFromFile = SchematicDataVersion.getVersionFor(fallback.getMaxDataVersion());
        }

        if (usedFallback == false && this.isFromDifferentMinecraftVersion())
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 8000, "litematica.message.warn.schematic_conversion.non_native_data_version",
                    String.valueOf(dataVersion), this.dataVersionFromFile.getMcVersionDisplayName());
        }

        this.setOutputMinecraftVersion(this.dataVersionFromFile.getMinecraftVersion());
    }

    protected void markDataModified(SchematicDataPiece data)
    {
        this.dirtyData.add(data);
    }

    protected boolean isFromDifferentMinecraftVersion()
    {
        return this.getCurrentSchematicDataVersion().getMinecraftVersion() != CURRENT_MINECRAFT_VERSION;
    }

    protected boolean needsVersionConversion()
    {
        MinecraftVersion versionFrom = this.getCurrentSchematicDataVersion().getMinecraftVersion();
        MinecraftVersion versionTo = this.requestedOutputMinecraftVersion;
        return versionFrom != versionTo;
    }

    /**
     * Returns true if there is NBT data read from the original file, and that
     * given piece of data has not been modified since loading from file.
     */
    protected final boolean canSaveCachedDataDirectly(SchematicDataPiece piece)
    {
        return this.cachedNbtDataFromFile != null && this.dirtyData.contains(piece) == false;
    }

    public final void loadFromCachedDataIfNeeded()
    {
        if (this.shouldLoadFromCachedData && this.cachedNbtDataFromFile != null)
        {
            this.fromCachedTag(this.cachedNbtDataFromFile);
            this.shouldLoadFromCachedData = false;
        }
    }

    @Nullable
    protected final NBTTagCompound getCachedDataFromFile()
    {
        return this.cachedNbtDataFromFile;
    }

    public long getTotalBlocksReadFromWorld()
    {
        return this.totalBlocksReadFromWorld;
    }

    public void setTotalBlocksReadFromWorld(long count)
    {
        this.totalBlocksReadFromWorld = count;
    }

    public static boolean isSizeValid(@Nullable Vec3i size)
    {
        return size != null && size.getX() > 0 && size.getY() > 0 && size.getZ() > 0;
    }

    protected Class<? extends ILitematicaBlockStateContainer> getContainerClass()
    {
        return LitematicaBlockStateContainerFull.class;
    }

    protected void copyContainerContents(ILitematicaBlockStateContainer from, ILitematicaBlockStateContainer to)
    {
        Vec3i sizeFrom = from.getSize();
        Vec3i sizeTo = to.getSize();
        final int sizeX = Math.min(sizeFrom.getX(), sizeTo.getX());
        final int sizeY = Math.min(sizeFrom.getY(), sizeTo.getY());
        final int sizeZ = Math.min(sizeFrom.getZ(), sizeTo.getZ());

        for (int y = 0; y < sizeY; ++y)
        {
            for (int z = 0; z < sizeZ; ++z)
            {
                for (int x = 0; x < sizeX; ++x)
                {
                    IBlockState state = from.getBlockState(x, y, z);
                    to.setBlockState(x, y, z, state);
                }
            }
        }
    }

    @Override
    public final ImmutableMap<String, ISchematicRegion> getRegions()
    {
        this.loadFromCachedDataIfNeeded();
        return this.getRegionsImpl();
    }

    @Override
    public final ISchematicRegion getSchematicRegion(String regionName)
    {
        this.loadFromCachedDataIfNeeded();
        return this.getSchematicRegionImpl(regionName);
    }

    /**
     * This method is called first, when reading data from NBT.
     * It allows the schematic to initialize any required custom things before
     * the other common methods are called.
     * This method should also set the schematic data version based on whatever
     * values are stored in the file for the given schematic type.
     * @param tag the compound tag read from the file
     * @return true if the data version was successfully detected and other early checks of the NBT data passed
     */
    protected abstract boolean setDataVersionFromTag(NBTTagCompound tag);

    protected abstract void fromCachedTag(NBTTagCompound tag);

    @Nullable
    protected abstract ISchematicRegion getSchematicRegionImpl(String regionName);

    /**
     * @return an immutable view of the sub-regions in this schematic
     */
    protected abstract ImmutableMap<String, ISchematicRegion> getRegionsImpl();

    @Override
    public final boolean fromTag(NBTTagCompound tag)
    {
        this.clear();

        if (this.setDataVersionFromTag(tag))
        {
            this.readMetadataFromTag(tag);
            this.cachedNbtDataFromFile = tag;
            this.shouldLoadFromCachedData = true;

            return true;
        }
        else
        {
            this.cachedNbtDataFromFile = null;
            this.shouldLoadFromCachedData = false;

            return false;
        }
    }

    protected void readMetadataFromTag(NBTTagCompound tag)
    {
        if (tag.hasKey("Metadata", Constants.NBT.TAG_COMPOUND))
        {
            this.getMetadata().fromTag(tag.getCompoundTag("Metadata"));
        }
    }

    protected NBTTagList convertBlockStatePalette(NBTTagList paletteTag, SchematicDataVersion versionFrom, MinecraftVersion versionTo)
    {
        BlockStateConverter converter = SchematicDataConversionManager.INSTANCE.getBlockStateConverter(versionFrom.getMinecraftVersion(), versionTo);

        if (converter != null)
        {
            paletteTag = converter.convertPalette(paletteTag);
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.error.schematic_conversion.missing_converter.block_states",
                    versionFrom.getMcVersionDisplayName(), String.valueOf(versionFrom.getDataVersion()), versionTo.getMcVersionDisplayName());
        }

        return paletteTag;
    }

    protected NBTTagList convertBlockEntityData(NBTTagList blockEntityList, String idTagName, SchematicDataVersion versionFrom, MinecraftVersion versionTo)
    {
        BlockEntityDataConverter converter = SchematicDataConversionManager.INSTANCE.getBlockEntityDataConverter(versionFrom.getMinecraftVersion(), versionTo);

        if (converter != null)
        {
            blockEntityList = converter.convertEntityNames(blockEntityList, idTagName);
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.error.schematic_conversion.missing_converter.block_entity_data",
                                        versionFrom.getMcVersionDisplayName(), String.valueOf(versionFrom.getDataVersion()), versionTo.getMcVersionDisplayName());
        }

        return blockEntityList;
    }

    protected NBTTagList convertBlockTickData(NBTTagList blockTickList, String idTagName, SchematicDataVersion versionFrom, MinecraftVersion versionTo)
    {
        BlockTickDataConverter converter = SchematicDataConversionManager.INSTANCE.getBlockTickDataConverter(versionFrom.getMinecraftVersion(), versionTo);

        if (converter != null)
        {
            blockTickList = converter.convertBlockNames(blockTickList, idTagName);
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.error.schematic_conversion.missing_converter.block_tick_data",
                                        versionFrom.getMcVersionDisplayName(), String.valueOf(versionFrom.getDataVersion()), versionTo.getMcVersionDisplayName());
        }

        return blockTickList;
    }

    protected NBTTagList convertEntityData(NBTTagList entityList, String idTagName, SchematicDataVersion versionFrom, MinecraftVersion versionTo)
    {
        EntityDataConverter converter = SchematicDataConversionManager.INSTANCE.getEntityDataConverter(versionFrom.getMinecraftVersion(), versionTo);

        if (converter != null)
        {
            entityList = converter.convertEntityNames(entityList, idTagName);
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.error.schematic_conversion.missing_converter.entity_data",
                                        versionFrom.getMcVersionDisplayName(), String.valueOf(versionFrom.getDataVersion()), versionTo.getMcVersionDisplayName());
        }

        return entityList;
    }

    protected NBTTagList convertBlockStatePaletteToCurrentGameVersion(NBTTagList paletteTag)
    {
        return this.convertBlockStatePalette(paletteTag, this.getCurrentSchematicDataVersion(), CURRENT_MINECRAFT_VERSION);
    }

    protected NBTTagList convertBlockEntityDataToCurrentGameVersion(NBTTagList blockEntityList, String idTagName)
    {
        return this.convertBlockEntityData(blockEntityList, idTagName, this.getCurrentSchematicDataVersion(), CURRENT_MINECRAFT_VERSION);
    }

    protected NBTTagList convertEntityDataToCurrentGameVersion(NBTTagList entityList, String idTagName)
    {
        return this.convertEntityData(entityList, idTagName, this.getCurrentSchematicDataVersion(), CURRENT_MINECRAFT_VERSION);
    }

    protected boolean readPaletteFromLitematicaFormatTag(NBTTagList tagList, ILitematicaBlockStatePalette palette)
    {
        final int size = tagList.tagCount();
        List<IBlockState> list = new ArrayList<>(size);

        for (int id = 0; id < size; ++id)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(id);
            IBlockState state = NBTUtil.readBlockState(tag);
            list.add(state);
        }

        return palette.setMapping(list);
    }

    protected ImmutableMap<BlockPos, NBTTagCompound> readBlockEntitiesFromListTag(NBTTagList tagList, @Nullable BlockEntityDataConverter beConverter, @Nullable InventoryDataConverter invConverter)
    {
        ImmutableMap.Builder<BlockPos, NBTTagCompound> builder = ImmutableMap.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            BlockPos pos = NBTUtils.readBlockPos(tag);

            if (pos != null && tag.isEmpty() == false)
            {
                tag = tag.copy();
                NBTUtils.removeBlockPosFromTag(tag);
                this.convertEntityTag(tag, "id", beConverter, invConverter);
                builder.put(pos, tag);
            }
        }

        return builder.build();
    }

    protected ImmutableList<EntityInfo> readEntitiesFromListTag(NBTTagList tagList, @Nullable EntityDataConverter entityConverter, @Nullable InventoryDataConverter invConverter)
    {
        ImmutableList.Builder<EntityInfo> builder = ImmutableList.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            Vec3d posVec = NBTUtils.readVec3dFromListTag(tag);

            if (posVec != null && tag.isEmpty() == false)
            {
                tag = tag.copy();
                this.convertEntityTag(tag, "id", entityConverter, invConverter);
                builder.add(new EntityInfo(posVec, tag));
            }
        }

        return builder.build();
    }

    protected void convertEntitiesInList(NBTTagList entityList, String idTagName, @Nullable EntityDataConverterBase entityConverter, @Nullable InventoryDataConverter invConverter)
    {
        if (entityConverter != null)
        {
            entityConverter.convertEntityNames(entityList, idTagName);
        }

        if (invConverter != null)
        {
            invConverter.convertAnyInventoryContentsInList(entityList, idTagName);
        }
    }

    protected void convertEntityTag(NBTTagCompound tag, String idTagName, @Nullable EntityDataConverterBase entityConverter, @Nullable InventoryDataConverter invConverter)
    {
        if (entityConverter != null)
        {
            entityConverter.convertName(tag, idTagName);
        }

        if (invConverter != null)
        {
            // Get the converted name
            idTagName = tag.getString(idTagName);
            invConverter.convertAnyInventoryContents(idTagName, tag);
        }
    }

    protected NBTTagCompound getMetadataTagForWriting(@Nullable NBTTagCompound cachedTag)
    {
        NBTTagCompound metaTag;

        if (cachedTag != null &&
            cachedTag.hasKey("Metadata", Constants.NBT.TAG_COMPOUND) &&
            this.canSaveCachedDataDirectly(SchematicDataPiece.METADATA))
        {
            metaTag = cachedTag.getCompoundTag("Metadata").copy();
        }
        else
        {
            metaTag = this.getMetadata().toTag();
        }

        return metaTag;
    }

    protected NBTTagList writePaletteToLitematicaFormatTag(ILitematicaBlockStatePalette palette)
    {
        final int size = palette.getPaletteSize();
        List<IBlockState> list = palette.getMapping();
        NBTTagList tagList = new NBTTagList();

        for (int id = 0; id < size; ++id)
        {
            NBTTagCompound tag = new NBTTagCompound();
            NBTUtil.writeBlockState(tag, list.get(id));
            tagList.appendTag(tag);
        }

        return tagList;
    }

    protected NBTTagList writeEntitiesToListTag(List<EntityInfo> entityList)
    {
        NBTTagList tagList = new NBTTagList();

        if (entityList.isEmpty() == false)
        {
            for (EntityInfo info : entityList)
            {
                tagList.appendTag(info.nbt.copy());
            }
        }

        return tagList;
    }

    protected NBTTagList writeBlockEntitiesToListTag(Map<BlockPos, NBTTagCompound> tileMap)
    {
        NBTTagList tagList = new NBTTagList();

        if (tileMap.isEmpty() == false)
        {
            for (Map.Entry<BlockPos, NBTTagCompound> entry : tileMap.entrySet())
            {
                NBTTagCompound tag = entry.getValue().copy();
                NBTUtils.writeBlockPosToTag(entry.getKey(), tag);
                tagList.appendTag(tag);
            }
        }

        return tagList;
    }

    public enum SchematicDataPiece
    {
        BLOCKS,
        ENTITIES,
        BLOCK_ENTITIES,
        BLOCK_TICKS,
        METADATA;
    }
}
