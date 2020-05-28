package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.EnumSet;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.mixin.IMixinDataFixer;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.schematic.conversion.ConversionUtils;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion.VersionClassification;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataPiece;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataVersion;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;

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

    protected NBTTagList convertBlockStatePaletteToCurrentGameVersion(NBTTagList paletteTag)
    {
        return ConversionUtils.convertBlockStatePalette(paletteTag, this.getCurrentSchematicDataVersion(), CURRENT_MINECRAFT_VERSION);
    }

    protected NBTTagList convertBlockEntityDataToCurrentGameVersion(NBTTagList blockEntityList, String idTagName)
    {
        return ConversionUtils.convertBlockEntityData(blockEntityList, idTagName, this.getCurrentSchematicDataVersion(), CURRENT_MINECRAFT_VERSION);
    }

    protected NBTTagList convertEntityDataToCurrentGameVersion(NBTTagList entityList, String idTagName)
    {
        return ConversionUtils.convertEntityData(entityList, idTagName, this.getCurrentSchematicDataVersion(), CURRENT_MINECRAFT_VERSION);
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
}
