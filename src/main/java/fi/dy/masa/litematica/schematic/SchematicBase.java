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
import fi.dy.masa.litematica.schematic.conversion.BlockStateConverter;
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
    public static final int MINECRAFT_DATA_VERSION = ((IMixinDataFixer) Minecraft.getMinecraft().getDataFixer()).getVersion();
    public static final MinecraftVersion CURRENT_GAME_SCHEMATIC_DATA_VERSION = MinecraftVersion.MC_1_12;

    protected final SchematicMetadata metadata = new SchematicMetadata();
    protected final EnumSet<SchematicDataPiece> dirtyData = EnumSet.noneOf(SchematicDataPiece.class);
    @Nullable protected final File schematicFile;
    @Nullable private NBTTagCompound cachedNbtDataFromFile;
    private boolean shouldLoadFromCachedData;

    /** This is the data version that the in-memory NBT data is currently in */
    protected SchematicDataVersion currentDataSchematicDataVersion = SchematicDataVersion.getVersionFor(CURRENT_GAME_SCHEMATIC_DATA_VERSION.getMaxDataVersion());
    /** This is the schematic data version that the data should be written to file as */
    protected MinecraftVersion requestedOutputMinecraftVersion = CURRENT_GAME_SCHEMATIC_DATA_VERSION;

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

    protected void setToCurrentGameVersion()
    {
        this.currentDataSchematicDataVersion = SchematicDataVersion.getVersionFor(CURRENT_GAME_SCHEMATIC_DATA_VERSION.getMaxDataVersion());
        this.requestedOutputMinecraftVersion = CURRENT_GAME_SCHEMATIC_DATA_VERSION;
    }

    @Override
    public void clear()
    {
        this.cachedNbtDataFromFile = null;
        this.shouldLoadFromCachedData = false;

        this.setToCurrentGameVersion();
    }

    protected final boolean getShouldLoadFromCachedData()
    {
        return this.shouldLoadFromCachedData;
    }

    @Override
    public SchematicDataVersion getCurrentSchematicDataVersion()
    {
        return this.currentDataSchematicDataVersion;
    }

    @Override
    public void setOutputMinecraftVersion(MinecraftVersion version)
    {
        this.requestedOutputMinecraftVersion = version;
    }

    protected void setCurrentDataVersionWithFallback(int dataVersion)
    {
        this.currentDataSchematicDataVersion = SchematicDataVersion.getVersionFor(dataVersion);
        boolean usedFallback = false;

        if (this.currentDataSchematicDataVersion == null)
        {
            VersionClassification classification = MinecraftVersion.getVersionClassification(dataVersion);
            MinecraftVersion fallback = CURRENT_GAME_SCHEMATIC_DATA_VERSION; // this version should never actually be used
            usedFallback = true;

            if (classification == VersionClassification.OLD)
            {
                fallback = MinecraftVersion.KNOWN_VERSIONS.get(0);

                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 8000, "litematica.error.schematic_conversion.unknown_data_version.old",
                        String.valueOf(dataVersion), fallback.getMcVersionDisplayName(), String.valueOf(fallback.getMaxDataVersion()));
            }
            else if (classification == VersionClassification.FUTURE)
            {
                fallback = MinecraftVersion.KNOWN_VERSIONS.get(MinecraftVersion.KNOWN_VERSIONS.size() - 1);

                String strLastKnown = fallback.getMcVersionDisplayName();
                InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 8000, "litematica.error.schematic_conversion.unknown_data_version.future",
                        String.valueOf(dataVersion), strLastKnown, fallback.getMcVersionDisplayName(), String.valueOf(fallback.getMaxDataVersion()));
            }

            this.currentDataSchematicDataVersion = SchematicDataVersion.getVersionFor(fallback.getMaxDataVersion());
        }

        if (usedFallback == false && this.isFromDifferentMinecraftVersion())
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 8000, "litematica.message.warn.schematic_conversion.non_native_data_version",
                    String.valueOf(dataVersion), this.currentDataSchematicDataVersion.getMcVersionDisplayName());
        }

        this.setOutputMinecraftVersion(this.currentDataSchematicDataVersion.getMinecraftVersion());
    }

    protected boolean isFromDifferentMinecraftVersion()
    {
        return this.getCurrentSchematicDataVersion().getMinecraftVersion() != CURRENT_GAME_SCHEMATIC_DATA_VERSION;
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
        if (this.shouldLoadFromCachedData && this.cachedNbtDataFromFile != null)
        {
            this.fromCachedTag(this.cachedNbtDataFromFile);
            this.shouldLoadFromCachedData = false;
        }

        return this.getRegionsImpl();
    }

    @Override
    public final ISchematicRegion getSchematicRegion(String regionName)
    {
        if (this.shouldLoadFromCachedData && this.cachedNbtDataFromFile != null)
        {
            this.fromCachedTag(this.cachedNbtDataFromFile);
            this.shouldLoadFromCachedData = false;
        }

        return this.getSchematicRegionImpl(regionName);
    }

    /**
     * This method is called first, when reading data from NBT.
     * It allows the schematic to initialize any required custom things before the common methods are called.
     * One such thing is the schematic data version read from the file.
     * @param tag
     * @return true on success, false if there is an irrecoverable error and reading should stop
     */
    protected abstract boolean initFromTag(NBTTagCompound tag);

    protected abstract boolean fromCachedTag(NBTTagCompound tag);

    @Nullable
    protected abstract ISchematicRegion getSchematicRegionImpl(String regionName);

    protected abstract ImmutableMap<String, ISchematicRegion> getRegionsImpl();

    @Override
    public final boolean fromTag(NBTTagCompound tag)
    {
        this.clear();

        if (this.initFromTag(tag))
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
            InfoUtils.printErrorMessage("litematica.error.schematic_read_from_file.missing_converter",
                    versionFrom.getMcVersionDisplayName(), String.valueOf(versionFrom.getDataVersion()), versionTo.getMcVersionDisplayName());
        }

        return paletteTag;
    }

    protected NBTTagList convertBlockStatePaletteToCurrentGameVersion(NBTTagList paletteTag)
    {
        return this.convertBlockStatePalette(paletteTag, this.getCurrentSchematicDataVersion(), CURRENT_GAME_SCHEMATIC_DATA_VERSION);
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

    protected ImmutableList<EntityInfo> readEntitiesFromListTag(NBTTagList tagList)
    {
        ImmutableList.Builder<EntityInfo> builder = ImmutableList.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound entityData = tagList.getCompoundTagAt(i);
            Vec3d posVec = NBTUtils.readVec3dFromListTag(entityData);

            if (posVec != null && entityData.isEmpty() == false)
            {
                builder.add(new EntityInfo(posVec, entityData));
            }
        }

        return builder.build();
    }

    protected ImmutableMap<BlockPos, NBTTagCompound> readBlockEntitiesFromListTag(NBTTagList tagList)
    {
        ImmutableMap.Builder<BlockPos, NBTTagCompound> builder = ImmutableMap.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            BlockPos pos = NBTUtils.readBlockPos(tag);
            NBTUtils.removeBlockPosFromTag(tag);

            if (pos != null && tag.isEmpty() == false)
            {
                builder.put(pos, tag);
            }
        }

        return builder.build();
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
                tagList.appendTag(info.nbt);
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
                NBTTagCompound tag = entry.getValue();
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
