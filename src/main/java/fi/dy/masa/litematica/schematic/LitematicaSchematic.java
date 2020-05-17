package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.selection.SelectionBox;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.mixin.IMixinNBTTagLongArray;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;

public class LitematicaSchematic extends SchematicBase
{
    public static final String FILE_NAME_EXTENSION = ".litematic";
    public static final int SCHEMATIC_VERSION = 4;

    private final Map<String, LitematicaBlockStateContainerFull> blockContainers = new HashMap<>();
    private final Map<String, ImmutableMap<BlockPos, NBTTagCompound>> blockEntities = new HashMap<>();
    private final Map<String, ImmutableMap<BlockPos, NextTickListEntry>> pendingBlockTicks = new HashMap<>();
    private final Map<String, ImmutableList<EntityInfo>> entities = new HashMap<>();
    private final Map<String, SubRegion> subRegions = new HashMap<>();

    private int subRegionCount = -1;
    private int litematicaSchematicVersionFromFile = -1;

    LitematicaSchematic(@Nullable File file)
    {
        super(file);
    }

    @Override
    public SchematicType<?> getType()
    {
        return SchematicType.LITEMATICA;
    }

    @Override
    public int getSubRegionCount()
    {
        return this.subRegionCount;
    }

    @Override
    public void clear()
    {
        super.clear();

        this.subRegions.clear();
        this.blockContainers.clear();
        this.blockEntities.clear();
        this.entities.clear();
        this.pendingBlockTicks.clear();
        this.getMetadata().clearModifiedSinceSaved();

        this.subRegionCount = -1;
    }

    @Override
    public ImmutableList<String> getRegionNames()
    {
        return ImmutableList.copyOf(this.subRegions.keySet());
    }

    @Override
    @Nullable
    protected ISchematicRegion getSchematicRegionImpl(String regionName)
    {
        return this.subRegions.containsKey(regionName) ? new LitematicaSubRegion(this, regionName) : null;
    }

    /**
     * Returns an immutable view of the sub-regions in this schematic
     * @return
     */
    @Override
    protected ImmutableMap<String, ISchematicRegion> getRegionsImpl()
    {
        ImmutableMap.Builder<String, ISchematicRegion> builder = ImmutableMap.builder();

        for (String regionName : this.subRegions.keySet())
        {
            builder.put(regionName, new LitematicaSubRegion(this, regionName));
        }

        return builder.build();
    }

    @Override
    public Vec3i getEnclosingSize()
    {
        ImmutableMap<String, ISchematicRegion> regions = this.getRegions();

        if (regions.isEmpty() == false)
        {
            if (regions.size() == 1)
            {
                for (ISchematicRegion region : regions.values())
                {
                    return PositionUtils.getAbsoluteAreaSize(region.getSize());
                }
            }
            else
            {
                List<Box> boxes = new ArrayList<>();

                for (ISchematicRegion region : regions.values())
                {
                    BlockPos pos = region.getPosition();
                    Vec3i end = PositionUtils.getRelativeEndPositionFromAreaSize(region.getSize());
                    Box box = new Box(pos, pos.add(end));
                    boxes.add(box);
                }

                return PositionUtils.getEnclosingAreaSize(boxes);
            }
        }

        return null;
    }

    /**
     * Sets the sub-region boxes for this schematic.
     * <b>Note:</b> This also clears any previous data, and this is meant to be
     * called before reading things from the world, when creating a schematic.
     * @param boxes the sub-region boxes, using absolute world coordinates
     * @param areaOrigin the area selection origin point
     */
    public void setSubRegions(List<SelectionBox> boxes, BlockPos areaOrigin)
    {
        this.clear();

        for (SelectionBox box : boxes)
        {
            String regionName = box.getName();
            BlockPos pos = box.getPos1().subtract(areaOrigin);
            Vec3i size = box.getSize();
            final int sizeX = Math.abs(size.getX());
            final int sizeY = Math.abs(size.getY());
            final int sizeZ = Math.abs(size.getZ());

            this.subRegions.put(regionName, new SubRegion(pos, size));

            try
            {
                this.blockContainers.put(regionName, new LitematicaBlockStateContainerFull(new Vec3i(sizeX, sizeY, sizeZ)));
            }
            catch (Exception e)
            {
                InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "TODO - Failed to create the block state container for sub-region: " + regionName);
                Litematica.logger.warn("Failed to create the block state container for sub-region '{}'", regionName, e.getMessage());
            }
        }
    }

    @Override
    public void readFrom(ISchematic other)
    {
        this.clear();

        ImmutableMap<String, ISchematicRegion> regions = other.getRegions();

        if (regions.isEmpty() == false)
        {
            for (Map.Entry<String, ISchematicRegion> regionEntry : regions.entrySet())
            {
                String regionName = regionEntry.getKey();
                ISchematicRegion region = regionEntry.getValue();
                ILitematicaBlockStateContainer containerOther = region.getBlockStateContainer();

                this.subRegions.put(regionName, new SubRegion(region.getPosition(), region.getSize()));

                if (containerOther instanceof LitematicaBlockStateContainerFull)
                {
                    this.blockContainers.put(regionName, (LitematicaBlockStateContainerFull) containerOther.copy());
                }
                else
                {
                    Vec3i size = containerOther.getSize();
                    LitematicaBlockStateContainerFull container = new LitematicaBlockStateContainerFull(size, false);
                    this.copyContainerContents(containerOther, container);
                    this.blockContainers.put(regionName, container);
                }

                ImmutableMap.Builder<BlockPos, NBTTagCompound> builderBlockEntities = ImmutableMap.builder();
                ImmutableMap.Builder<BlockPos, NextTickListEntry> builderBlockTicks = ImmutableMap.builder();
                ImmutableList.Builder<EntityInfo> builderEntities = ImmutableList.builder();

                region.getBlockEntityMap().entrySet().forEach((entry) -> builderBlockEntities.put(entry.getKey(), entry.getValue().copy()));
                region.getEntityList().forEach((info) -> builderEntities.add(info.copy()));
                builderBlockTicks.putAll(region.getBlockTickMap());

                this.blockEntities.put(regionName, builderBlockEntities.build());
                this.pendingBlockTicks.put(regionName, builderBlockTicks.build());
                this.entities.put(regionName, builderEntities.build());
            }

            this.getMetadata().copyFrom(other.getMetadata());
        }
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setTag("Metadata", this.getMetadata().toTag());
        nbt.setTag("Regions", this.writeSubRegionsToNBT());

        if (this.requestedOutputMinecraftVersion != CURRENT_GAME_SCHEMATIC_DATA_VERSION)
        {
            nbt.setInteger("Version", SCHEMATIC_VERSION);
            nbt.setInteger("MinecraftDataVersion", this.requestedOutputMinecraftVersion.getMaxDataVersion());
        }
        else
        {
            nbt.setInteger("Version", SCHEMATIC_VERSION);
            nbt.setInteger("MinecraftDataVersion", MINECRAFT_DATA_VERSION);
        }

        return nbt;
    }

    @Override
    protected boolean initFromTag(NBTTagCompound tag)
    {
        if (tag.hasKey("Version", Constants.NBT.TAG_INT))
        {
            this.litematicaSchematicVersionFromFile = tag.getInteger("Version");

            int dataVersion;

            if (tag.hasKey("MinecraftDataVersion", Constants.NBT.TAG_INT))
            {
                dataVersion = tag.getInteger("MinecraftDataVersion");
            }
            else
            {
                // The early 1.13.2 builds (between 2018-11 .. 2019-06) didn't have the MinecraftDataVersion field yet
                if (this.litematicaSchematicVersionFromFile == 5)
                {
                    // And also additionally, the saved value was actually accidentally hard coded to
                    // that of 1.13.2 until some 1.15.1 builds >_> So there is no real way to fix that case...
                    dataVersion = MinecraftVersion.MC_1_13.getMaxDataVersion();
                }
                else
                {
                    dataVersion = MinecraftVersion.MC_1_12.getMinDataVersion(); // 1.12.0 because... /shrug
                }
            }

            this.setCurrentDataVersionWithFallback(dataVersion);
            this.subRegionCount = tag.getCompoundTag("Regions").getKeySet().size();

            return true;
        }

        InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_version_information");

        return false;
    }

    @Override
    protected boolean fromCachedTag(NBTTagCompound tag)
    {
        final int version = this.litematicaSchematicVersionFromFile;

        //if (version >= 1 && version <= SCHEMATIC_VERSION)
        if (version >= 1 && version <= 5)
        {
            final boolean needsVersionConversion = this.isFromDifferentMinecraftVersion();

            this.readMetadataFromTag(tag);
            this.readSubRegionsFromTag(tag, version, needsVersionConversion);

            return true;
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.unsupported_schematic_version", version);
        }

        return false;
    }

    private NBTTagCompound writeSubRegionsToNBT()
    {
        NBTTagCompound wrapper = new NBTTagCompound();
        MinecraftVersion versionFrom = this.getCurrentSchematicDataVersion().getMinecraftVersion();
        MinecraftVersion versionTo = this.requestedOutputMinecraftVersion;
        boolean needsVersionConversion = versionFrom != versionTo;

        if (this.getShouldLoadFromCachedData())
        {
            // FIXME remove this, it was just for testing
            this.getRegions();
        }

        if (this.blockContainers.isEmpty() == false)
        {
            for (String regionName : this.blockContainers.keySet())
            {
                LitematicaBlockStateContainerFull blockContainer = (LitematicaBlockStateContainerFull) this.blockContainers.get(regionName);
                Map<BlockPos, NBTTagCompound> tileMap = this.blockEntities.get(regionName);
                List<EntityInfo> entityList = this.entities.get(regionName);
                Map<BlockPos, NextTickListEntry> pendingTicks = this.pendingBlockTicks.get(regionName);

                NBTTagCompound tag = new NBTTagCompound();
                NBTTagList paletteTag = this.writePaletteToLitematicaFormatTag(blockContainer.getPalette());

                if (needsVersionConversion)
                {
                    paletteTag = this.convertBlockStatePalette(paletteTag, this.getCurrentSchematicDataVersion(), versionTo);
                }

                tag.setTag("BlockStatePalette", paletteTag);
                tag.setTag("BlockStates", new NBTTagLongArray(blockContainer.getBackingLongArray()));

                if (tileMap != null)
                {
                    NBTTagList listBlockEntities = this.writeBlockEntitiesToListTag(tileMap);

                    if (needsVersionConversion)
                    {
                        
                    }

                    tag.setTag("TileEntities", listBlockEntities);
                }

                if (pendingTicks != null)
                {
                    NBTTagList listBlockTicks = this.writeBlockTicksToNBT(pendingTicks);

                    if (needsVersionConversion)
                    {
                        
                    }

                    tag.setTag("PendingBlockTicks", listBlockTicks);
                }

                // The entity list will not exist, if takeEntities is false when creating the schematic
                if (entityList != null)
                {
                    NBTTagList listEntities = this.writeEntitiesToListTag(entityList);

                    if (needsVersionConversion)
                    {
                        
                    }

                    tag.setTag("Entities", listEntities);
                }

                SubRegion region = this.subRegions.get(regionName);
                tag.setTag("Position", NBTUtils.createBlockPosTag(region.pos));
                tag.setTag("Size", NBTUtils.createBlockPosTag(region.size));

                wrapper.setTag(regionName, tag);
            }
        }

        return wrapper;
    }

    private NBTTagList writeBlockTicksToNBT(Map<BlockPos, NextTickListEntry> tickMap)
    {
        NBTTagList tagList = new NBTTagList();

        if (tickMap.isEmpty() == false)
        {
            for (NextTickListEntry entry : tickMap.values())
            {
                ResourceLocation rl = Block.REGISTRY.getNameForObject(entry.getBlock());

                if (rl != null)
                {
                    NBTTagCompound tag = new NBTTagCompound();

                    tag.setString("Block", rl.toString());
                    tag.setInteger("Priority", entry.priority);
                    tag.setInteger("Time", (int) entry.scheduledTime);
                    tag.setInteger("x", entry.position.getX());
                    tag.setInteger("y", entry.position.getY());
                    tag.setInteger("z", entry.position.getZ());

                    tagList.appendTag(tag);
                }
            }
        }

        return tagList;
    }

    private boolean readSubRegionsFromTag(NBTTagCompound tag, int version, boolean needsVersionConversion)
    {
        tag = tag.getCompoundTag("Regions");

        for (String regionName : tag.getKeySet())
        {
            if (tag.getTag(regionName).getId() == Constants.NBT.TAG_COMPOUND)
            {
                NBTTagCompound regionTag = tag.getCompoundTag(regionName);
                BlockPos regionPos = NBTUtils.readBlockPos(regionTag.getCompoundTag("Position"));
                BlockPos regionSize = NBTUtils.readBlockPos(regionTag.getCompoundTag("Size"));

                if (regionPos != null && regionSize != null)
                {
                    this.subRegions.put(regionName, new SubRegion(regionPos, regionSize));

                    NBTTagList listBlockEntities = regionTag.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
                    NBTTagList listEntities = regionTag.getTagList("Entities", Constants.NBT.TAG_COMPOUND);

                    if (needsVersionConversion)
                    {
                        // TODO
                    }

                    if (version >= 2)
                    {
                        this.blockEntities.put(regionName, this.readBlockEntitiesFromListTag(listBlockEntities));
                        this.entities.put(regionName, this.readEntitiesFromListTag(listEntities));
                    }
                    else if (version == 1)
                    {
                        this.blockEntities.put(regionName, this.readTileEntitiesFromNBT_v1(listBlockEntities));
                        this.entities.put(regionName, this.readEntitiesFromNBT_v1(listEntities));
                    }

                    if (version >= 3)
                    {
                        NBTTagList listBlockTicks = regionTag.getTagList("PendingBlockTicks", Constants.NBT.TAG_COMPOUND);

                        if (needsVersionConversion)
                        {
                            // TODO
                        }

                        this.pendingBlockTicks.put(regionName, this.readBlockTicksFromNBT(listBlockTicks));
                    }

                    NBTBase nbtBase = regionTag.getTag("BlockStates");

                    // There are no convenience methods in NBTTagCompound yet in 1.12, so we'll have to do it the ugly way...
                    if (nbtBase != null && nbtBase.getId() == Constants.NBT.TAG_LONG_ARRAY)
                    {
                        Vec3i size = new Vec3i(Math.abs(regionSize.getX()), Math.abs(regionSize.getY()), Math.abs(regionSize.getZ()));
                        NBTTagList paletteTag = regionTag.getTagList("BlockStatePalette", Constants.NBT.TAG_COMPOUND);
                        long[] blockStateArr = ((IMixinNBTTagLongArray) nbtBase).getArray();
                        final int paletteSize = paletteTag.tagCount();

                        LitematicaBlockStateContainerFull container = LitematicaBlockStateContainerFull.createContainer(paletteSize, blockStateArr, size);

                        if (container == null)
                        {
                            InfoUtils.printErrorMessage("litematica.error.schematic_read_from_file_failed.region_container",
                                    regionName, this.getFile() != null ? this.getFile().getName() : "<null>");
                            return false;
                        }

                        // Loading a schematic from a different MC version, convert the palette
                        if (needsVersionConversion)
                        {
                            paletteTag = this.convertBlockStatePaletteToCurrentGameVersion(paletteTag);
                        }

                        this.readPaletteFromLitematicaFormatTag(paletteTag, container.getPalette());
                        this.blockContainers.put(regionName, container);
                    }
                    else
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private ImmutableMap<BlockPos, NextTickListEntry> readBlockTicksFromNBT(NBTTagList tagList)
    {
        ImmutableMap.Builder<BlockPos, NextTickListEntry> builder = ImmutableMap.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);

            if (tag.hasKey("Block", Constants.NBT.TAG_STRING) &&
                tag.hasKey("Time", Constants.NBT.TAG_ANY_NUMERIC)) // XXX these were accidentally saved as longs in version 3
            {
                Block block = Block.REGISTRY.getObject(new ResourceLocation(tag.getString("Block")));

                if (block != null && block != Blocks.AIR)
                {
                    BlockPos pos = new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
                    NextTickListEntry entry = new NextTickListEntry(pos, block);
                    entry.setPriority(tag.getInteger("Priority"));

                    // Note: the time is a relative delay at this point
                    entry.setScheduledTime(tag.getInteger("Time"));

                    builder.put(pos, entry);
                }
            }
        }

        return builder.build();
    }

    private ImmutableList<EntityInfo> readEntitiesFromNBT_v1(NBTTagList tagList)
    {
        ImmutableList.Builder<EntityInfo> builder = ImmutableList.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            Vec3d posVec = NBTUtils.readVec3d(tag);
            NBTTagCompound entityData = tag.getCompoundTag("EntityData");

            if (posVec != null && entityData.isEmpty() == false)
            {
                // Update the correct position to the Entity NBT, where it is stored in version 2
                NBTUtils.writeVec3dToListTag(posVec, entityData);
                builder.add(new EntityInfo(posVec, entityData));
            }
        }

        return builder.build();
    }

    private ImmutableMap<BlockPos, NBTTagCompound> readTileEntitiesFromNBT_v1(NBTTagList tagList)
    {
        ImmutableMap.Builder<BlockPos, NBTTagCompound> builder = ImmutableMap.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            NBTTagCompound tileNbt = tag.getCompoundTag("TileNBT");

            // Note: This within-schematic relative position is not inside the tile tag!
            BlockPos pos = NBTUtils.readBlockPos(tag);

            if (pos != null && tileNbt.isEmpty() == false)
            {
                builder.put(pos, tileNbt);
            }
        }

        return builder.build();
    }

    public static Boolean isValidSchematic(NBTTagCompound tag)
    {
        if (tag.hasKey("Version", Constants.NBT.TAG_INT) &&
            tag.hasKey("Regions", Constants.NBT.TAG_COMPOUND) &&
            tag.hasKey("Metadata", Constants.NBT.TAG_COMPOUND))
        {
            return true;
        }

        return false;
    }

    @Nullable
    public static LitematicaSchematic createFromFile(File dir, String fileName)
    {
        if (fileName.endsWith(FILE_NAME_EXTENSION) == false)
        {
            fileName = fileName + FILE_NAME_EXTENSION;
        }

        File file = new File(dir, fileName);
        LitematicaSchematic schematic = new LitematicaSchematic(file);

        return schematic.readFromFile() ? schematic : null;
    }

    public static class LitematicaSubRegion implements ISchematicRegion
    {
        private final LitematicaSchematic schematic;
        private final String regionName;

        public LitematicaSubRegion(LitematicaSchematic schematic, String regionName)
        {
            this.schematic = schematic;
            this.regionName = regionName;
        }

        @Override
        public BlockPos getPosition()
        {
            return this.schematic.subRegions.get(this.regionName).pos;
        }

        @Override
        public Vec3i getSize()
        {
            return this.schematic.subRegions.get(this.regionName).size;
        }

        @Override
        public ILitematicaBlockStateContainer getBlockStateContainer()
        {
            return this.schematic.blockContainers.get(this.regionName);
        }

        @Override
        public ImmutableList<EntityInfo> getEntityList()
        {
            return this.schematic.entities.computeIfAbsent(this.regionName, (name) -> { return ImmutableList.of(); });
        }

        @Override
        public ImmutableMap<BlockPos, NBTTagCompound> getBlockEntityMap()
        {
            return this.schematic.blockEntities.computeIfAbsent(this.regionName, (name) -> { return ImmutableMap.of(); });
        }

        @Override
        public ImmutableMap<BlockPos, NextTickListEntry> getBlockTickMap()
        {
            return this.schematic.pendingBlockTicks.computeIfAbsent(this.regionName, (name) -> { return ImmutableMap.of(); });
        }

        @Override
        public void setEntityList(List<EntityInfo> list)
        {
            this.schematic.entities.put(this.regionName, ImmutableList.copyOf(list));
            this.schematic.dirtyData.add(SchematicDataPiece.ENTITIES);
        }

        @Override
        public void setBlockEntityMap(Map<BlockPos, NBTTagCompound> map)
        {
            this.schematic.blockEntities.put(this.regionName, ImmutableMap.copyOf(map));
            this.schematic.dirtyData.add(SchematicDataPiece.BLOCK_ENTITIES);
        }

        @Override
        public void setBlockTickMap(Map<BlockPos, NextTickListEntry> map)
        {
            this.schematic.pendingBlockTicks.put(this.regionName, ImmutableMap.copyOf(map));
            this.schematic.dirtyData.add(SchematicDataPiece.BLOCK_TICKS);
        }
    }
}
