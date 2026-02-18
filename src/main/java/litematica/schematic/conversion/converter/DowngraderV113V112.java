package litematica.schematic.conversion.converter;

import litematica.Litematica;
import litematica.schematic.container.ArrayBlockContainer;
import litematica.schematic.conversion.SchematicDataConverter;
import litematica.schematic.data.EntityData;
import malilib.gui.BaseScreen;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.ListData;
import malilib.util.game.BlockUtils;
import malilib.util.game.MinecraftVersion;
import malilib.util.position.BlockPos;
import malilib.util.world.BlockState;
import malilib.util.world.ScheduledBlockTickData;
import net.minecraft.init.Blocks;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class DowngraderV113V112 extends SchematicDataConverter
{
    public static DowngraderV113V112 INSTANCE = new DowngraderV113V112();

    private Map<CompoundData, CompoundData> stateMap = new HashMap<>();
    private Map<String, StateFixer> fixerMap = new HashMap<>();

    public static MinecraftVersion versionFrom = MinecraftVersion.MC_1_13;
    public static MinecraftVersion versionTo = MinecraftVersion.MC_1_12;

    private DowngraderV113V112()
    {
        Optional<Map<CompoundData, CompoundData>> stateMap = BlockStateMapReader.readMap("block_state_map_113_to_112.json", "1.13", "1.12");
        if (stateMap.isPresent())
        {
            this.stateMap = stateMap.get();
        }
        else
        {
            MessageDispatcher.error("failed to read block_state_map_113_to_112.json");
        }
        this.fixerMap.put("minecraft:flower_pot",              FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_poppy",            FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_blue_orchid",      FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_allium",           FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_azure_bluet",      FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_red_tulip",        FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_orange_tulip",     FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_white_tulip",      FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_pink_tulip",       FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_oxeye_daisy",      FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_dandelion",        FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_oak_sapling",      FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_spruce_sapling",   FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_birch_sapling",    FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_jungle_sapling",   FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_acacia_sapling",   FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_dark_oak_sapling", FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_brown_mushroom",   FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_red_mushroom",     FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_dead_bush",        FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_fern",             FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:potted_cactus",           FIXER_FLOWERPOT);
        this.fixerMap.put("minecraft:note_block",              FIXER_NOTE_BLOCK);
    }


    // right inverse converter: if a blockstate exists in 1.13, and a 1.12 blockstate gets converted
    // to it by vanilla datafixer, then this converter should try to restore the 1.13 blockstate to
    // the 1.12 blockstate. If multiple 1.12 blockstates map to the 1.13 state, pick the most reasonable one.
    // duplicates from merges:
    // dirt/coarse dirt, flowing water/lava, leaves, shrub (tallgrass 31:0), double stone slab,
    // smooth_stone, smooth (red)sandstone, smooth quartz, mushroom blocks, pumpkin/melon stem
    // flower pot, skull, powered redstone comparator, double_plant
    public void convertContainer(
        ListData paletteTag,
        ArrayBlockContainer container,
        Map<BlockPos, CompoundData> blockEntityMap,
        Map<BlockPos, ScheduledBlockTickData> blockTickMap
    ) {
        final int paletteSize = paletteTag.size();
        //ListData paletteTagOut = new ListData(Constants.NBT.TAG_COMPOUND);

        ArrayList<String> failedStates = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        ListData paletteTagOriginal = paletteTag.copy();
        boolean needBlockFixer = false;

        for (int i = 0; i < paletteSize; ++i)
        {
            CompoundData tag = paletteTag.getCompoundAt(i);
            CompoundData convertedTag = this.stateMap.get(tag);

            if (convertedTag != null)
            {
                //System.out.printf("converted: %s => %s\n", tag, convertedTag);
                paletteTag.set(i, convertedTag.copy());
                //paletteTagOut.add(convertedTag.copy());
                ++successCount;

                String blockName = tag.getString("Name");

                if (fixerMap.containsKey(blockName))
                {
                    needBlockFixer = true;
                }
            }
            else
            {
                System.out.printf("FAILED: %s => %s\n", tag, convertedTag);
                failedStates.add(tag.toString());
                //paletteTagOut.add(tag.copy());
                paletteTag.set(i, BlockUtils.writeBlockState(new CompoundData(), BlockState.of(Blocks.BARRIER.getDefaultState())));
                ++failCount;
            }
        }
        // TODO fix duplciate blockstates in palette from downgrade, e.g. merge "flower_pot"s

        if (needBlockFixer) {
            for (int x = 0; x < container.getSize().getX(); x++) {
                for (int y = 0; y < container.getSize().getY(); y++) {
                    for (int z = 0; z < container.getSize().getZ(); z++) {
                        int i = container.getPaletteId(x, y, z);
                        CompoundData tag = paletteTagOriginal.getCompoundAt(i);
                        String blockName = tag.getString("Name");
                        StateFixer fixer = fixerMap.get(blockName);
                        if (fixer != null) {
                            fixer.fixState(new BlockPos(x, y, z), container, blockEntityMap, tag);
                        }
                    }
                }
            }
        }

        if (failCount > 0)
        {
            String verFrom = versionFrom.displayName;
            String verTo = versionTo.displayName;
            String strSu = String.valueOf(successCount);
            String strFa = String.valueOf(failCount);
            MessageDispatcher.warning("litematica.message.warn.schematic_conversion.palette_conversion_failures", verFrom, verTo, strSu, strFa);
            MessageDispatcher.error(String.join("\n", failedStates));
            BaseScreen.openPopupScreen(new SaveConversionFailureLogScreen(failedStates));
        }
    }

    public void convertEntityList(List<EntityData> entityList)
    {

    }

    static final Map<String, Pair<String, Integer>> flowerPotDataMap = new HashMap<>();
    static {
        flowerPotDataMap.put("minecraft:flower_pot",              Pair.of("minecraft:air", 0));
        flowerPotDataMap.put("minecraft:potted_poppy",            Pair.of("minecraft:red_flower", 0));
        flowerPotDataMap.put("minecraft:potted_blue_orchid",      Pair.of("minecraft:red_flower", 1));
        flowerPotDataMap.put("minecraft:potted_allium",           Pair.of("minecraft:red_flower", 2));
        flowerPotDataMap.put("minecraft:potted_azure_bluet",      Pair.of("minecraft:red_flower", 3));
        flowerPotDataMap.put("minecraft:potted_red_tulip",        Pair.of("minecraft:red_flower", 4));
        flowerPotDataMap.put("minecraft:potted_orange_tulip",     Pair.of("minecraft:red_flower", 5));
        flowerPotDataMap.put("minecraft:potted_white_tulip",      Pair.of("minecraft:red_flower", 6));
        flowerPotDataMap.put("minecraft:potted_pink_tulip",       Pair.of("minecraft:red_flower", 7));
        flowerPotDataMap.put("minecraft:potted_oxeye_daisy",      Pair.of("minecraft:red_flower", 8));
        flowerPotDataMap.put("minecraft:potted_dandelion",        Pair.of("minecraft:yellow_flower", 0));
        flowerPotDataMap.put("minecraft:potted_oak_sapling",      Pair.of("minecraft:sapling", 0));
        flowerPotDataMap.put("minecraft:potted_spruce_sapling",   Pair.of("minecraft:sapling", 1));
        flowerPotDataMap.put("minecraft:potted_birch_sapling",    Pair.of("minecraft:sapling", 2));
        flowerPotDataMap.put("minecraft:potted_jungle_sapling",   Pair.of("minecraft:sapling", 3));
        flowerPotDataMap.put("minecraft:potted_acacia_sapling",   Pair.of("minecraft:sapling", 4));
        flowerPotDataMap.put("minecraft:potted_dark_oak_sapling", Pair.of("minecraft:sapling", 5));
        flowerPotDataMap.put("minecraft:potted_brown_mushroom",   Pair.of("minecraft:brown_mushroom", 0));
        flowerPotDataMap.put("minecraft:potted_red_mushroom",     Pair.of("minecraft:red_mushroom", 0));
        flowerPotDataMap.put("minecraft:potted_dead_bush",        Pair.of("minecraft:deadbush", 0));
        flowerPotDataMap.put("minecraft:potted_fern",             Pair.of("minecraft:tallgrass", 2));
        flowerPotDataMap.put("minecraft:potted_cactus",           Pair.of("minecraft:cactus", 0));
    }
    final StateFixer FIXER_FLOWERPOT = (pos, container, blockEntityMap, originalTag) -> {
        Pair<String, Integer> beData = flowerPotDataMap.get(originalTag.getString("Name"));

        CompoundData flowerPotBeTag = new CompoundData();
        flowerPotBeTag.putString("Item", beData.getLeft());
        flowerPotBeTag.putInt("Data", beData.getRight());
        flowerPotBeTag.putString("id", "minecraft:flower_pot");
        flowerPotBeTag.putInt("x", pos.getX());
        flowerPotBeTag.putInt("y", pos.getY());
        flowerPotBeTag.putInt("z", pos.getZ());
        blockEntityMap.put(pos, flowerPotBeTag);
    };

    final StateFixer FIXER_NOTE_BLOCK = (pos, container, blockEntityMap, originalTag) -> {
        CompoundData properties = originalTag.getCompound("Properties");
        if (properties != null)
        {
            byte note = 0;
            boolean powered = Boolean.parseBoolean(properties.getString("powered"));
            try {
                note = Byte.parseByte(properties.getString("note"));
            } catch (NumberFormatException e) {
                Litematica.LOGGER.error(e);
            }
            // instrument is not stored in 1.12

            CompoundData noteBlockBeTag = new CompoundData();
            noteBlockBeTag.putByte("note", note);
            noteBlockBeTag.putBoolean("powered", powered);
            noteBlockBeTag.putString("id", "minecraft:noteblock");
            noteBlockBeTag.putInt("x", pos.getX());
            noteBlockBeTag.putInt("y", pos.getY());
            noteBlockBeTag.putInt("z", pos.getZ());
            blockEntityMap.put(pos, noteBlockBeTag);
        }
    };

    private void fixSkullBlockEntity() {

    }

    private void fixNoteBlockEntity() {

    }
    private void fixBedBlockEntity() {

    }

    private void fixBannerBlockEntity() {

    }
}

interface StateFixer {
    void fixState(
        BlockPos pos,
        ArrayBlockContainer container,
        Map<BlockPos, CompoundData> blockEntityMap,
        CompoundData originalTag
    );
}

