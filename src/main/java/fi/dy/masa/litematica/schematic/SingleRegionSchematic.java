package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.ILitematicaPalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerBase;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.schematic.conversion.ConversionUtils;
import fi.dy.masa.litematica.schematic.conversion.IListTagDataConverter;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataConversionManager;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataPiece;
import fi.dy.masa.litematica.schematic.conversion.converter.BlockEntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.EntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.InventoryDataConverter;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;

public abstract class SingleRegionSchematic extends SchematicBase implements ISchematicRegion
{
    protected ImmutableMap<BlockPos, NBTTagCompound> blockEntities = ImmutableMap.of();
    protected ImmutableMap<BlockPos, NextTickListEntry> pendingBlockTicks = ImmutableMap.of();
    protected ImmutableList<EntityInfo> entities = ImmutableList.of();
    protected ILitematicaBlockStateContainer blockContainer;
    protected BlockPos regionPos = BlockPos.ORIGIN;
    private Vec3i regionSize = Vec3i.NULL_VECTOR;

    public SingleRegionSchematic(File file)
    {
        super(file);

        this.getMetadata().setRegionCount(1);
    }

    @Override
    public void clear()
    {
        super.clear();

        this.entities = ImmutableList.of();
        this.blockEntities = ImmutableMap.of();
        this.pendingBlockTicks = ImmutableMap.of();
        this.metadata.clearModifiedSinceSaved();
    }

    @Override
    public ImmutableList<String> getRegionNames()
    {
        return ImmutableList.of(this.getMetadata().getName());
    }

    @Override
    protected ImmutableMap<String, ISchematicRegion> getRegionsImpl()
    {
        return ImmutableMap.of(this.getMetadata().getName(), this);
    }

    @Override
    protected ISchematicRegion getSchematicRegionImpl(String regionName)
    {
        return this;
    }

    @Override
    public BlockPos getPosition()
    {
        return this.regionPos;
    }

    @Override
    public Vec3i getSize()
    {
        return this.regionSize;
    }

    protected boolean setSize(@Nullable Vec3i size)
    {
        this.regionSize = size;

        if (size != null)
        {
            if (isSizeValid(size) == false)
            {
                InfoUtils.printErrorMessage("litematica.message.error.schematic_read.invalid_or_missing_size_value", size.getX(), size.getY(), size.getZ());
                return false;
            }

            this.getMetadata().setEnclosingSize(size);
            this.getMetadata().setTotalVolume(PositionUtils.getAreaVolume(size));

            return true;
        }

        return false;
    }

    protected void createEmptyContainer(Vec3i size)
    {
        this.blockContainer = new LitematicaBlockStateContainerFull(size);
    }

    @Override
    public ILitematicaBlockStateContainer getBlockStateContainer()
    {
        return this.blockContainer;
    }

    @Override
    public ImmutableList<EntityInfo> getEntityList()
    {
        return this.entities;
    }

    @Override
    public ImmutableMap<BlockPos, NBTTagCompound> getBlockEntityMap()
    {
        return this.blockEntities;
    }

    @Override
    public ImmutableMap<BlockPos, NextTickListEntry> getBlockTickMap()
    {
        return this.pendingBlockTicks;
    }

    @Override
    public void setEntityList(List<EntityInfo> list)
    {
        this.entities = ImmutableList.copyOf(list);
        this.markDataModified(SchematicDataPiece.ENTITIES);
    }

    @Override
    public void setBlockEntityMap(Map<BlockPos, NBTTagCompound> map)
    {
        this.blockEntities = ImmutableMap.copyOf(map);
        this.markDataModified(SchematicDataPiece.BLOCK_ENTITIES);
    }

    @Override
    public void setBlockTickMap(Map<BlockPos, NextTickListEntry> map)
    {
        this.pendingBlockTicks = ImmutableMap.copyOf(map);
        this.markDataModified(SchematicDataPiece.BLOCK_TICKS);
    }

    @Override
    public Vec3i getEnclosingSize()
    {
        return this.getSize();
    }

    @Override
    public void readFrom(ISchematic other)
    {
        ImmutableMap<String, ISchematicRegion> regions = other.getRegions();

        if (regions.isEmpty() == false)
        {
            if (Configs.Generic.DEBUG_MESSAGES.getBooleanValue()) System.out.printf("SingleRegionSchematic::readFrom(other)\n");
            this.clear();

            Vec3i size = other.getEnclosingSize();

            if (size != null)
            {
                try
                {
                    if (this.setSize(size))
                    {
                        this.readFrom(regions, other.wasDataModified(SchematicDataPiece.BLOCKS));

                        this.getMetadata().copyFrom(other.getMetadata());
                        this.getMetadata().setRegionCount(1);
                    }
                }
                catch (Exception e)
                {
                    Litematica.logger.warn("Exception while reading schematic contents from another schematic", e);
                }
            }
        }
    }

    protected void readFrom(ImmutableMap<String, ISchematicRegion> regions, boolean dataModified)
    {
        Pair<BlockPos, BlockPos> pair = PositionUtils.getEnclosingAreaCornersForRegions(regions.values());

        if (pair == null)
        {
            return;
        }

        final BlockPos minCorner = pair.getLeft();

        // Create a palette that contains all the values from the containers in the other schematic
        final ILitematicaPalette<NBTTagCompound> combinedTagPalette = ILitematicaPalette.createCombinedPalette(regions.values());

        if (regions.size() == 1)
        {
            for (ISchematicRegion region : regions.values())
            {
                ILitematicaBlockStateContainer containerOther = region.getBlockStateContainer();

                if (this.getContainerClass() == containerOther.getClass())
                {
                    if (Configs.Generic.DEBUG_MESSAGES.getBooleanValue()) System.out.printf("SingleRegionSchematic::readFrom(regions), single, same, palette size: %d\n", combinedTagPalette.getPaletteSize());
                    this.blockContainer = containerOther.copy();
                }
                else
                {
                    if (Configs.Generic.DEBUG_MESSAGES.getBooleanValue()) System.out.printf("SingleRegionSchematic::readFrom(regions), single, different, palette size: %d\n", combinedTagPalette.getPaletteSize());
                    this.createEmptyContainer(containerOther.getSize());
                    this.blockContainer.setTagPalette(combinedTagPalette);
                    this.copyContainerContents(containerOther, this.blockContainer);
                }
            }
        }
        else
        {
            if (regions.size() > 1 && dataModified)
            {
                this.createEmptyContainer(this.getSize());
            }

            if (Configs.Generic.DEBUG_MESSAGES.getBooleanValue()) System.out.printf("SingleRegionSchematic::readFrom(regions), multi to single, palette size: %d\n", combinedTagPalette.getPaletteSize());
            // Set the pre-constructed tag palette to this schematic's container, so that the contents can be
            // copied over using raw ID mapping constructed based on this tag palette
            int bits = LitematicaBlockStateContainerBase.getBitsForCapacity(combinedTagPalette.getPaletteSize());
            this.blockContainer = new LitematicaBlockStateContainerFull(this.getSize(), bits, null);
            this.blockContainer.setTagPalette(combinedTagPalette);

            for (ISchematicRegion region : regions.values())
            {
                ILitematicaBlockStateContainer containerOther = region.getBlockStateContainer();

                // This is the relative position of this sub-region within the new single region enclosing schematic volume
                Vec3i regionOffset = this.getRegionOffset(region, minCorner);

                containerOther.copyContentsTo(this.blockContainer, regionOffset, dataModified == false);
            }
        }

        final ImmutableMap.Builder<BlockPos, NBTTagCompound> builderBlockEntities = ImmutableMap.builder();
        final ImmutableMap.Builder<BlockPos, NextTickListEntry> builderBlockTicks = ImmutableMap.builder();
        final ImmutableList.Builder<EntityInfo> builderEntities = ImmutableList.builder();

        for (ISchematicRegion region : regions.values())
        {
            // No offset for this sub-region, use the positions in the maps without modifications
            if (region.getPosition().equals(BlockPos.ORIGIN))
            {
                region.getBlockEntityMap().forEach((key, value) -> builderBlockEntities.put(key, value.copy()));
                region.getBlockTickMap().forEach(builderBlockTicks::put);
                region.getEntityList().forEach((info) -> builderEntities.add(info.copy()));
            }
            else
            {
                // This is the relative position of this sub-region within the new single region enclosing schematic volume
                Vec3i regionOffsetBlocks = this.getRegionOffset(region, minCorner);

                region.getBlockEntityMap().forEach((key, value) -> {
                    BlockPos pos = key.add(regionOffsetBlocks);
                    builderBlockEntities.put(pos, value.copy());
                });

                region.getBlockTickMap().forEach((key, value) -> {
                    BlockPos pos = key.add(regionOffsetBlocks);
                    builderBlockTicks.put(pos, value);
                });

                // The entity positions are not relative to the sub-region's minimum corner,
                // but to the sub-region's origin point, whichever corner that is at.
                Vec3i regionOffsetEntities = region.getPosition().subtract(minCorner);

                region.getEntityList().forEach((info) -> {
                    Vec3d pos = info.pos.add(regionOffsetEntities.getX(), regionOffsetEntities.getY(), regionOffsetEntities.getZ());
                    NBTTagCompound nbt = info.nbt.copy();
                    NBTUtils.writeVec3dToListTag(pos, nbt);
                    builderEntities.add(new EntityInfo(pos, nbt));
                });
            }
        }

        this.setBlockEntityMap(builderBlockEntities.build());
        this.setBlockTickMap(builderBlockTicks.build());
        this.setEntityList(builderEntities.build());
    }

    protected Vec3i getRegionOffset(ISchematicRegion region, BlockPos minCorner)
    {
        // Get the offset from the region's block state container origin
        // (the minimum corner of the region) to the enclosing area's origin/minimum corner.
        BlockPos regionPos = region.getPosition();
        Vec3i endRel = PositionUtils.getRelativeEndPositionFromAreaSize(region.getSize());
        BlockPos regionEnd = regionPos.add(endRel);
        BlockPos regionMin = fi.dy.masa.malilib.util.PositionUtils.getMinCorner(regionPos, regionEnd);
        BlockPos regionOffset = regionMin.subtract(minCorner);

        return regionOffset;
    }

    @Override
    protected final void fromCachedTag(NBTTagCompound tag)
    {
        this.setSize(this.readSizeFromTag(tag));

        if (isSizeValid(this.regionSize) == false)
        {
            InfoUtils.printErrorMessage("litematica.message.error.schematic_read.invalid_or_missing_size", this.getFile().getAbsolutePath());
            return;
        }

        this.createEmptyContainer(this.regionSize);

        final boolean needsVersionConversion = this.isFromDifferentMinecraftVersion();

        if (this.readBlocksFromTag(tag, needsVersionConversion))
        {
            this.readAllEntityData(tag, needsVersionConversion);
            this.readMetadataFromTag(tag);
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.message.error.schematic_read.missing_or_invalid_data", this.getFile().getAbsolutePath());
        }
    }

    protected void readAllEntityData(NBTTagCompound tag, boolean needsVersionConversion)
    {
        final MinecraftVersion versionFrom = this.getCurrentSchematicDataVersion().getMinecraftVersion();
        final MinecraftVersion versionTo = CURRENT_MINECRAFT_VERSION;
        final BlockEntityDataConverter beConverter = needsVersionConversion ? SchematicDataConversionManager.INSTANCE.getBlockEntityDataConverter(versionFrom, versionTo) : null;
        final InventoryDataConverter invConverter = needsVersionConversion ? SchematicDataConversionManager.INSTANCE.getInventoryDataConverter(versionFrom, versionTo) : null;
        final EntityDataConverter entityConverter = needsVersionConversion ? SchematicDataConversionManager.INSTANCE.getEntityDataConverter(versionFrom, versionTo) : null;

        this.blockEntities = this.readBlockEntitiesFromTag(tag, beConverter, invConverter);
        this.entities = this.readEntitiesFromTag(tag, entityConverter, invConverter);
    }

    @Override
    protected void readMetadataFromTag(NBTTagCompound tag)
    {
        super.readMetadataFromTag(tag);

        // Not stored in metadata yet
        if (this.blockContainer != null && this.getMetadata().getTotalBlocks() < 0)
        {
            long totalBlocks = this.blockContainer.getTotalBlockCount();
            this.getMetadata().setTotalBlocks(totalBlocks);
        }
    }

    @Nullable protected abstract Vec3i readSizeFromTag(NBTTagCompound tag);

    protected abstract boolean readBlocksFromTag(NBTTagCompound tag, boolean needsVersionConversion);

    protected abstract ImmutableMap<BlockPos, NBTTagCompound> readBlockEntitiesFromTag(NBTTagCompound tag, @Nullable BlockEntityDataConverter beConverter, @Nullable InventoryDataConverter invConverter);

    protected abstract ImmutableList<EntityInfo> readEntitiesFromTag(NBTTagCompound tag, @Nullable EntityDataConverter entityConverter, @Nullable InventoryDataConverter invConverter);

    protected NBTTagCompound toTagBase()
    {
        NBTTagCompound cachedTag = null;

        // No rebuild actions done, use the cached data originally read from file
        if (this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCKS) &&
            this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCK_ENTITIES))
        {
            cachedTag = this.getCachedDataFromFile();
        }

        final NBTTagCompound tag = cachedTag != null ? cachedTag.copy() : new NBTTagCompound();

        MinecraftVersion versionFrom = this.getCurrentSchematicDataVersion().getMinecraftVersion();
        MinecraftVersion versionTo = this.requestedOutputMinecraftVersion;
        IListTagDataConverter beConverter = ConversionUtils.createBlockEntityListConverter(versionFrom, versionTo);
        IListTagDataConverter entityConverter = ConversionUtils.createEntityListConverter(versionFrom, versionTo);

        this.writeBlocksToTag(tag, cachedTag, this.needsVersionConversion());
        this.writeBlockEntitiesToTag(tag, cachedTag, beConverter);
        this.writeEntitiesToTag(tag, cachedTag, entityConverter);
        this.writeMetadataToTag(tag, cachedTag);

        this.onWriteToTag(tag, cachedTag);

        return tag;
    }

    protected abstract void writeBlocksToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag, boolean needsConversion);

    protected abstract void writeBlockEntitiesToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag, @Nullable IListTagDataConverter converter);

    protected abstract void writeEntitiesToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag, @Nullable IListTagDataConverter converter);

    protected abstract void writeMetadataToTag(NBTTagCompound tag, @Nullable NBTTagCompound cachedTag);

    protected void onWriteToTag(NBTTagCompound tagOut, @Nullable NBTTagCompound cachedTag)
    {
    }
}
