package litematica.scheduler.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;

import litematica.schematic.*;
import malilib.util.game.wrap.BlockWrap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.NonNullList;

import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.tag.CompoundData;
import malilib.util.game.MinecraftVersion;
import malilib.util.game.wrap.GameWrap;
import malilib.util.position.BlockPos;
import malilib.util.position.ChunkPos;
import malilib.util.position.Vec3i;
import malilib.util.world.BlockState;
import litematica.render.infohud.InfoHud;
import litematica.scheduler.tasks.TaskProcessChunkBase;
import litematica.schematic.container.BlockContainer;
import litematica.selection.AreaSelection;
import litematica.selection.SelectionBox;
import litematica.util.PositionUtils;

public class LocalCreateDebugItemSchematicTask extends TaskProcessChunkBase
{
    protected final AreaSelection area;
    protected final BlockPos origin;
    protected final ImmutableMap<String, SelectionBox> subRegions;
    protected final ArrayListMultimap<ChunkPos, SelectionBox> selectionBoxesPerChunk;
    protected final Consumer<Schematic> schematicListener;
    protected long totalBlocks;
    protected int totalEntities;
    protected long totalBlockEntities;
    protected long totalBlockTicks;

    public LocalCreateDebugItemSchematicTask(Consumer<Schematic> schematicListener)
    {
        super("litematica.hud.task_name.local_create_schematic");

        NonNullList<ItemStack> items = NonNullList.create();
        for (Item item : Item.REGISTRY) {
            NonNullList<ItemStack> subitems = NonNullList.create();
            item.getSubItems(CreativeTabs.SEARCH, subitems);
            if (subitems.isEmpty()) {
                items.add(item.getDefaultInstance());
            } else {
                items.addAll(subitems);
            }
        }
        //System.out.println(items);
        List<List<ItemStack>> chestInventories = new ArrayList<>();
        List<ItemStack> currentList = new ArrayList<>();
        for (ItemStack stack : items) {
            currentList.add(stack);
            if (currentList.size() == 27) {
                chestInventories.add(currentList);
                currentList = new ArrayList<>();
            }
        }
        int numChests = chestInventories.size();

        String regionName = "debug items";
        Vec3i schemSize = new Vec3i(numChests, 1, 1);
        BlockContainer container = LitematicaSchematic.createDefaultBlockContainer(schemSize);
        HashMap<BlockPos, CompoundData> blockEntityMap = new HashMap<>();
        for (int i = 0; i < numChests; i++) {
            if (i % 2 == 0) {
                container.setBlockState(i, 0, 0, BlockState.of(Blocks.CHEST.getDefaultState()));
            } else {
                container.setBlockState(i, 0, 0, BlockState.of(Blocks.TRAPPED_CHEST.getDefaultState()));
            }
            TileEntityChest chestBlockEntity = new TileEntityChest();
            for (int j = 0; j < chestInventories.get(i).size(); j++) {
                chestBlockEntity.setInventorySlotContents(j, chestInventories.get(i).get(j));
            }
            blockEntityMap.put(new BlockPos(i, 0, 0), BlockWrap.writeBlockEntityToTag(chestBlockEntity));
        }

        SchematicRegion region = new SchematicRegion(
            BlockPos.ORIGIN, schemSize,
            container, blockEntityMap, new HashMap<>(), new ArrayList<>(),
            BaseSchematic.CURRENT_MINECRAFT_DATA_VERSION);

        this.area = new AreaSelection();
        this.area.addSelectionBox(new SelectionBox(BlockPos.ORIGIN, BlockPos.of(schemSize), regionName), true);
        Collection<SelectionBox> allBoxes = this.area.getAllSelectionBoxes();

        ImmutableMap<String, SchematicRegion> regions = ImmutableMap.of(regionName, region);

        this.schematicListener = schematicListener;
        this.origin = this.area.getEffectiveOrigin();
        this.subRegions = this.area.getAllSelectionBoxesMap();
        this.selectionBoxesPerChunk = PositionUtils.getPerChunkBoxes(allBoxes);

        this.totalBlocks = numChests;
        this.totalBlockEntities = numChests;
        this.totalEntities = 0;
        this.totalBlockTicks = 0;

        Optional<Schematic> schematicOpt = SchematicType.LITEMATICA.createSchematicFromRegions(regions);
        if (schematicOpt.isPresent())
        {
            Schematic schematic = schematicOpt.get();
            this.setMetadataValues(schematic.getMetadata(), regions.size());
            this.schematicListener.accept(schematic);
        }
        else
        {
            MessageDispatcher.error(8000).translate("litematica.message.error.save_schematic.failed_to_create_schematic");
        }
    }

    protected void setMetadataValues(SchematicMetadata meta, int regionCount)
    {
        Collection<SelectionBox> boxes = this.subRegions.values();
        meta.setRegionCount(regionCount);
        meta.setEnclosingSize(PositionUtils.getEnclosingAreaSizeOfBoxes(boxes));
        meta.setTotalVolume(PositionUtils.getTotalVolume(boxes));
        meta.setTotalBlocks(this.totalBlocks);
        meta.setEntityCount(this.totalEntities);
        meta.setBlockEntityCount(this.totalBlockEntities);
        meta.setBlockTickCount(this.totalBlockTicks);

        meta.setAuthor(GameWrap.getPlayerName());
        meta.setSchematicName(this.area.getName());
        meta.setOriginalOrigin(this.origin);

        meta.setTimeCreated(System.currentTimeMillis());
        meta.setMinecraftVersion(MinecraftVersion.CURRENT_VERSION);
    }

    @Override
    protected void onStop()
    {
        if (this.finished == false)
        {
            MessageDispatcher.warning().translate("litematica.message.error.schematic_save.interrupted");
        }

        InfoHud.getInstance().removeInfoHudRenderer(this, false);

        this.notifyListener();
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos) {
        return false;
    }

    @Override
    protected boolean processChunk(ChunkPos pos) {
        return false;
    }
}
