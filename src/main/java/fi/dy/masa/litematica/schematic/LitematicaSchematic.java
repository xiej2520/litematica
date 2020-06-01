package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.schematic.conversion.ConversionUtils;
import fi.dy.masa.litematica.schematic.conversion.IListTagDataConverter;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataConversionManager;
import fi.dy.masa.litematica.schematic.conversion.SchematicDataPiece;
import fi.dy.masa.litematica.schematic.conversion.converter.BlockEntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.BlockTickDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.EntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.InventoryDataConverter;
import fi.dy.masa.litematica.schematic.util.SchematicDataUtils;
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
        this.litematicaSchematicVersionFromFile = -1;
    }

    public static boolean isValidSchematic(NBTTagCompound tag)
    {
        return tag.hasKey("Version", Constants.NBT.TAG_INT) &&
                tag.hasKey("Regions", Constants.NBT.TAG_COMPOUND) &&
                tag.hasKey("Metadata", Constants.NBT.TAG_COMPOUND);
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

    @Override
    public int getSubRegionCount()
    {
        return this.subRegionCount;
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

        return Vec3i.NULL_VECTOR;
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

                final ImmutableMap.Builder<BlockPos, NBTTagCompound> builderBlockEntities = ImmutableMap.builder();
                final ImmutableMap.Builder<BlockPos, NextTickListEntry> builderBlockTicks = ImmutableMap.builder();
                final ImmutableList.Builder<EntityInfo> builderEntities = ImmutableList.builder();

                region.getBlockEntityMap().forEach((key, value) -> builderBlockEntities.put(key, value.copy()));
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
    protected boolean setDataVersionFromTag(NBTTagCompound tag)
    {
        if (tag.hasKey("Version", Constants.NBT.TAG_INT))
        {
            this.litematicaSchematicVersionFromFile = tag.getInteger("Version");

            int dataVersion;

            if (tag.hasKey("MinecraftDataVersion", Constants.NBT.TAG_INT))
            {
                dataVersion = tag.getInteger("MinecraftDataVersion");
            }
            // Old schematics didn't have the DataVersion value yet...
            else
            {
                // The early 1.13.2 builds (between 2018-11 .. 2019-06) didn't have the MinecraftDataVersion field yet >_>
                if (this.litematicaSchematicVersionFromFile == 5)
                {
                    // And also additionally, the saved value was actually accidentally hard coded to
                    // that of 1.13.2 until some 1.15.1 builds >_> So there is no real way to fix that case automatically...
                    dataVersion = MinecraftVersion.MC_1_13_X.getMaxDataVersion();
                }
                else
                {
                    dataVersion = MinecraftVersion.MC_1_12_X.getMinDataVersion(); // 1.12.0 because... /shrug
                }
            }

            this.setCurrentDataVersionWithFallback(dataVersion);

            // This needs to be set here, so that the schematic browser can show
            // the correct sub region count, without parsing the cached NBT data.
            this.subRegionCount = tag.getCompoundTag("Regions").getKeySet().size();

            return true;
        }

        InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.no_schematic_version_information");

        return false;
    }

    @Override
    protected void fromCachedTag(NBTTagCompound tag)
    {
        final int version = this.litematicaSchematicVersionFromFile;

        if (version >= 1 && version <= MinecraftVersion.getLatestKnownVersion().getSchematicVersion())
        {
            boolean needsVersionConversion = this.isFromDifferentMinecraftVersion();
            if (Configs.Generic.DEBUG_MESSAGES.getBooleanValue()) System.out.printf("LitematicaSchematic::fromCachedTag(), needsConv: %s\n", needsVersionConversion);
            this.readSubRegionsFromTag(tag, version, needsVersionConversion);
        }
        else
        {
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, "litematica.error.schematic_load.unsupported_schematic_version", version);
        }
    }

    private void readSubRegionsFromTag(NBTTagCompound tag, int version, boolean needsVersionConversion)
    {
        final MinecraftVersion versionFrom = this.getCurrentSchematicDataVersion().getMinecraftVersion();
        final MinecraftVersion versionTo = CURRENT_MINECRAFT_VERSION;
        final BlockEntityDataConverter beConverter = needsVersionConversion ? SchematicDataConversionManager.INSTANCE.getBlockEntityDataConverter(versionFrom, versionTo) : null;
        final BlockTickDataConverter blockTickConverter = needsVersionConversion ? SchematicDataConversionManager.INSTANCE.getBlockTickDataConverter(versionFrom, versionTo) : null;
        final InventoryDataConverter invConverter = needsVersionConversion ? SchematicDataConversionManager.INSTANCE.getInventoryDataConverter(versionFrom, versionTo) : null;
        final EntityDataConverter entityConverter = needsVersionConversion ? SchematicDataConversionManager.INSTANCE.getEntityDataConverter(versionFrom, versionTo) : null;

        tag = tag.getCompoundTag("Regions");

        for (String regionName : tag.getKeySet())
        {
            if (tag.getTag(regionName).getId() != Constants.NBT.TAG_COMPOUND)
            {
                Litematica.logger.warn("Invalid sub-region tag type for region '{}'", regionName);
                continue;
            }

            NBTTagCompound regionTag = tag.getCompoundTag(regionName);
            BlockPos regionPos = NBTUtils.readBlockPos(regionTag.getCompoundTag("Position"));
            BlockPos regionSize = NBTUtils.readBlockPos(regionTag.getCompoundTag("Size"));

            if (regionPos == null || regionSize == null)
            {
                Litematica.logger.warn("Invalid sub-region position or size for region '{}'", regionName);
                continue;
            }

            this.subRegions.put(regionName, new SubRegion(regionPos, regionSize));

            NBTTagList listBlockEntities = regionTag.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
            NBTTagList listEntities = regionTag.getTagList("Entities", Constants.NBT.TAG_COMPOUND);

            if (needsVersionConversion && (beConverter != null || invConverter != null))
            {
                listBlockEntities = listBlockEntities.copy();
            }

            if (version >= 2)
            {
                this.blockEntities.put(regionName, SchematicDataUtils.readBlockEntitiesFromListTag(listBlockEntities, beConverter, invConverter));
                this.entities.put(regionName, SchematicDataUtils.readEntitiesFromListTag(listEntities, entityConverter, invConverter));
            }
            else if (version == 1)
            {
                this.blockEntities.put(regionName, this.readTileEntitiesFromNBT_v1(listBlockEntities, beConverter, invConverter));
                this.entities.put(regionName, this.readEntitiesFromNBT_v1(listEntities, entityConverter, invConverter));
            }

            if (version >= 3)
            {
                NBTTagList listBlockTicks = regionTag.getTagList("PendingBlockTicks", Constants.NBT.TAG_COMPOUND);

                if (needsVersionConversion && blockTickConverter != null)
                {
                    listBlockTicks = listBlockTicks.copy();
                }

                this.pendingBlockTicks.put(regionName, this.readBlockTicksFromNBT(listBlockTicks, blockTickConverter));
            }

            NBTBase nbtBase = regionTag.getTag("BlockStates");

            // There are no convenience methods in NBTTagCompound yet in 1.12, so we'll have to do it the ugly way...
            if (nbtBase != null && nbtBase.getId() == Constants.NBT.TAG_LONG_ARRAY)
            {
                Vec3i size = new Vec3i(Math.abs(regionSize.getX()), Math.abs(regionSize.getY()), Math.abs(regionSize.getZ()));
                NBTTagList paletteTagOriginal = regionTag.getTagList("BlockStatePalette", Constants.NBT.TAG_COMPOUND).copy();
                NBTTagList paletteTagConverted = paletteTagOriginal;
                long[] blockStateArr = ((IMixinNBTTagLongArray) nbtBase).getArray();
                final int paletteSize = paletteTagOriginal.tagCount();

                LitematicaBlockStateContainerFull container = LitematicaBlockStateContainerFull.createContainer(paletteSize, blockStateArr, size);

                if (container == null)
                {
                    InfoUtils.printErrorMessage("litematica.error.schematic_read_from_file_failed.region_container",
                                                regionName, this.getFile() != null ? this.getFile().getName() : "<null>");
                    return;
                }

                // Loading a schematic from a different MC version, convert the palette
                if (needsVersionConversion)
                {
                    paletteTagConverted = this.convertBlockStatePaletteToCurrentGameVersion(paletteTagOriginal);
                }

                SchematicDataUtils.readPaletteFromLitematicaFormatTag(paletteTagConverted, paletteTagOriginal, container);
                this.blockContainers.put(regionName, container);
            }
            else
            {
                return;
            }
        }
    }

    private ImmutableMap<BlockPos, NextTickListEntry> readBlockTicksFromNBT(NBTTagList tagList, @Nullable BlockTickDataConverter blockTickConverter)
    {
        ImmutableMap.Builder<BlockPos, NextTickListEntry> builder = ImmutableMap.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);

            if (tag.hasKey("Block", Constants.NBT.TAG_STRING) &&
                tag.hasKey("Time", Constants.NBT.TAG_ANY_NUMERIC)) // XXX these were accidentally saved as longs in version 3
            {
                String name = tag.getString("Block");

                if (blockTickConverter != null)
                {
                    name = blockTickConverter.convertBlockName(name);
                }

                Block block = Block.REGISTRY.getObject(new ResourceLocation(name));

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

    private ImmutableList<EntityInfo> readEntitiesFromNBT_v1(NBTTagList tagList, @Nullable EntityDataConverter entityConverter, @Nullable InventoryDataConverter invConverter)
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
                tag = tag.copy();
                ConversionUtils.convertEntityTag(tag, "id", entityConverter, invConverter);
                // Update the correct position to the Entity NBT, where it is stored in version 2
                NBTUtils.writeVec3dToListTag(posVec, entityData);
                builder.add(new EntityInfo(posVec, entityData));
            }
        }

        return builder.build();
    }

    private ImmutableMap<BlockPos, NBTTagCompound> readTileEntitiesFromNBT_v1(NBTTagList tagList, @Nullable BlockEntityDataConverter beConverter, @Nullable InventoryDataConverter invConverter)
    {
        ImmutableMap.Builder<BlockPos, NBTTagCompound> builder = ImmutableMap.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound wrapperTag = tagList.getCompoundTagAt(i);
            NBTTagCompound tag = wrapperTag.getCompoundTag("TileNBT");

            // Note: This within-schematic relative position is not inside the tile tag!
            BlockPos pos = NBTUtils.readBlockPos(wrapperTag);

            if (pos != null && tag.isEmpty() == false)
            {
                tag = tag.copy();
                ConversionUtils.convertEntityTag(tag, "id", beConverter, invConverter);
                builder.put(pos, tag);
            }
        }

        return builder.build();
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setTag("Metadata", this.getMetadata().toTag());
        nbt.setTag("Regions", this.writeSubRegionsToNBT());
        nbt.setInteger("Version", this.requestedOutputMinecraftVersion.getSchematicVersion());
        nbt.setInteger("MinecraftDataVersion", this.requestedOutputMinecraftVersion.getMaxDataVersion());

        /*
        if (this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCKS) &&
            this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCK_ENTITIES))
        {
            nbt.setInteger("Version", this.litematicaSchematicVersionFromFile);
        }
        else
        {
            nbt.setInteger("Version", CURRENT_SCHEMATIC_VERSION);
        }
        */

        return nbt;
    }

    private NBTTagCompound writeSubRegionsToNBT()
    {
        final MinecraftVersion versionFrom = this.getCurrentSchematicDataVersion().getMinecraftVersion();
        final MinecraftVersion versionTo = this.requestedOutputMinecraftVersion;
        IListTagDataConverter beConverter = ConversionUtils.createBlockEntityListConverter(versionFrom, versionTo);
        IListTagDataConverter entityConverter = ConversionUtils.createEntityListConverter(versionFrom, versionTo);
        IListTagDataConverter blockTickConverter = ConversionUtils.createBlockTickListConverter(versionFrom, versionTo);

        NBTTagCompound regionsContainerTagNew = new NBTTagCompound();
        NBTTagCompound cachedRegionsContainerTag = null;
        Set<String> regionNames;

        // No rebuild actions done, use the cached data originally read from file
        if (this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCKS) &&
            this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCK_ENTITIES))
        {
            cachedRegionsContainerTag = this.getCachedDataFromFile().getCompoundTag("Regions");
            regionNames = cachedRegionsContainerTag.getKeySet();
        }
        else
        {
            regionNames = this.blockContainers.keySet();
        }

        for (final String regionName : regionNames)
        {
            @Nullable
            NBTTagCompound oldRegionTag = null;
            NBTTagCompound regionTag = new NBTTagCompound();

            if (cachedRegionsContainerTag != null && cachedRegionsContainerTag.hasKey(regionName, Constants.NBT.TAG_COMPOUND))
            {
                oldRegionTag = cachedRegionsContainerTag.getCompoundTag(regionName);
            }

            SubRegion region = this.subRegions.get(regionName);

            if (region != null)
            {
                regionTag.setTag("Position", NBTUtils.createBlockPosTag(region.pos));
                regionTag.setTag("Size", NBTUtils.createBlockPosTag(region.size));
            }
            else if (oldRegionTag != null)
            {
                regionTag.setTag("Position", oldRegionTag.getCompoundTag("Position"));
                regionTag.setTag("Size", oldRegionTag.getCompoundTag("Size"));
            }
            else
            {
                continue;
            }

            this.writeBlocksToRegionTag(regionName, regionTag, oldRegionTag);

            SchematicDataUtils.writeListDataToTag(() -> this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCK_ENTITIES),
                                                  regionTag, oldRegionTag, "TileEntities", "id", beConverter,
                                                  () -> this.writeBlockEntitiesToListTag(regionName));

            SchematicDataUtils.writeListDataToTag(() -> this.canSaveCachedDataDirectly(SchematicDataPiece.ENTITIES),
                                                  regionTag, oldRegionTag, "Entities", "id", entityConverter,
                                                  () -> this.writeEntitiesToListTag(regionName));

            SchematicDataUtils.writeListDataToTag(() -> this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCK_TICKS),
                                                  regionTag, oldRegionTag, "PendingBlockTicks", "Block", blockTickConverter,
                                                  () -> this.writeBlockTicksToListTag(regionName));

            regionsContainerTagNew.setTag(regionName, regionTag);
        }

        return regionsContainerTagNew;
    }

    private void writeBlocksToRegionTag(String regionName, NBTTagCompound regionTag, @Nullable NBTTagCompound cachedRegionTag)
    {
        NBTTagList paletteTag = null;
        NBTBase blockStatesTag = null;

        if (cachedRegionTag != null && this.canSaveCachedDataDirectly(SchematicDataPiece.BLOCKS))
        {
            blockStatesTag = cachedRegionTag.getTag("BlockStates");
            paletteTag = cachedRegionTag.getTagList("BlockStatePalette", Constants.NBT.TAG_COMPOUND);
        }
        else
        {
            LitematicaBlockStateContainerFull blockContainer = this.blockContainers.get(regionName);

            if (blockContainer != null)
            {
                blockStatesTag = new NBTTagLongArray(blockContainer.getBackingLongArray());
                paletteTag = SchematicDataUtils.writePaletteToLitematicaFormatTag(blockContainer.getPalette());
            }
        }

        if (paletteTag != null && blockStatesTag != null)
        {
            if (this.needsVersionConversion())
            {
                paletteTag = ConversionUtils.convertBlockStatePalette(paletteTag, this.getCurrentSchematicDataVersion(), this.requestedOutputMinecraftVersion);
            }

            regionTag.setTag("BlockStatePalette", paletteTag);
            regionTag.setTag("BlockStates", blockStatesTag);
        }
    }

    private NBTTagList writeBlockEntitiesToListTag(String regionName)
    {
        Map<BlockPos, NBTTagCompound> map = this.blockEntities.get(regionName);
        return map != null ? SchematicDataUtils.writeBlockEntitiesToListTag(map) : null;
    }

    private NBTTagList writeEntitiesToListTag(String regionName)
    {
        List<EntityInfo> list = this.entities.get(regionName);
        return list != null ? SchematicDataUtils.writeEntitiesToListTag(list) : null;
    }

    private NBTTagList writeBlockTicksToListTag(String regionName)
    {
        Map<BlockPos, NextTickListEntry> map = this.pendingBlockTicks.get(regionName);
        return map != null ? this.writeBlockTicksToListTag(map) : null;
    }

    private NBTTagList writeBlockTicksToListTag(Map<BlockPos, NextTickListEntry> tickMap)
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
            return this.schematic.entities.computeIfAbsent(this.regionName, (name) -> ImmutableList.of());
        }

        @Override
        public ImmutableMap<BlockPos, NBTTagCompound> getBlockEntityMap()
        {
            return this.schematic.blockEntities.computeIfAbsent(this.regionName, (name) -> ImmutableMap.of());
        }

        @Override
        public ImmutableMap<BlockPos, NextTickListEntry> getBlockTickMap()
        {
            return this.schematic.pendingBlockTicks.computeIfAbsent(this.regionName, (name) -> ImmutableMap.of());
        }

        @Override
        public void setEntityList(List<EntityInfo> list)
        {
            this.schematic.entities.put(this.regionName, ImmutableList.copyOf(list));
            this.schematic.markDataModified(SchematicDataPiece.ENTITIES);
        }

        @Override
        public void setBlockEntityMap(Map<BlockPos, NBTTagCompound> map)
        {
            this.schematic.blockEntities.put(this.regionName, ImmutableMap.copyOf(map));
            this.schematic.markDataModified(SchematicDataPiece.BLOCK_ENTITIES);
        }

        @Override
        public void setBlockTickMap(Map<BlockPos, NextTickListEntry> map)
        {
            this.schematic.pendingBlockTicks.put(this.regionName, ImmutableMap.copyOf(map));
            this.schematic.markDataModified(SchematicDataPiece.BLOCK_TICKS);
        }
    }
}
