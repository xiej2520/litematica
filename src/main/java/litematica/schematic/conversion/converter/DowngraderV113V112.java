package litematica.schematic.conversion.converter;

import com.google.common.collect.ImmutableMap;
import litematica.Litematica;
import litematica.schematic.container.ArrayBlockContainer;
import litematica.schematic.conversion.SchematicDataConverter;
import litematica.schematic.data.EntityData;
import malilib.gui.BaseScreen;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.Constants;
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

    protected Map<CompoundData, CompoundData> stateMap = new HashMap<>();

    public static MinecraftVersion versionFrom = MinecraftVersion.MC_1_13;
    public static MinecraftVersion versionTo = MinecraftVersion.MC_1_12;

    protected DowngraderV113V112()
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
            String flattenedBlockName = tag.getString("Name");

            if (this.convertBlockStateData(tag))
            {
                System.out.printf("converted: %s => %s\n", paletteTagOriginal.getCompoundAt(i), tag);
                if (unfixers.containsKey(flattenedBlockName))
                {
                    needBlockFixer = true;
                }
                ++successCount;
            }
            else
            {
                System.out.printf("FAILED: %s\n", tag);
                failedStates.add(tag.toString());
                paletteTag.set(i, BlockUtils.writeBlockState(new CompoundData(), BlockState.of(Blocks.BARRIER.getDefaultState())));
                failCount += 1;
            }
        }

        if (needBlockFixer) {
            for (int x = 0; x < container.getSize().getX(); x++) {
                for (int y = 0; y < container.getSize().getY(); y++) {
                    for (int z = 0; z < container.getSize().getZ(); z++) {
                        int i = container.getPaletteId(x, y, z);
                        CompoundData tag = paletteTagOriginal.getCompoundAt(i);
                        String blockName = tag.getString("Name");
                        UnfixBlockEntityCreator fixer = unfixers.get(blockName);
                        if (fixer != null) {
                            fixer.recreateBlockEntity(new BlockPos(x, y, z), container, blockEntityMap, tag);
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

    public boolean convertBlockStateData(CompoundData data) {
        CompoundData convertedData = this.stateMap.get(data);
        if (convertedData == null) {
            return false;
        }
        data.putString("Name", convertedData.getString("Name"));
        data.remove("Properties");
        if (convertedData.contains("Properties", Constants.NBT.TAG_COMPOUND)) {
            data.put("Properties", convertedData.getCompound("Properties").copy());
        }
        return true;
    }

    public void convertEntityList(List<EntityData> entityList)
    {

    }

    static final ImmutableMap<String, Pair<String, Integer>> flowerPotData =
        ImmutableMap.<String, Pair<String, Integer>>builder()
        .put("minecraft:flower_pot",              Pair.of("minecraft:air", 0))
        .put("minecraft:potted_poppy",            Pair.of("minecraft:red_flower", 0))
        .put("minecraft:potted_blue_orchid",      Pair.of("minecraft:red_flower", 1))
        .put("minecraft:potted_allium",           Pair.of("minecraft:red_flower", 2))
        .put("minecraft:potted_azure_bluet",      Pair.of("minecraft:red_flower", 3))
        .put("minecraft:potted_red_tulip",        Pair.of("minecraft:red_flower", 4))
        .put("minecraft:potted_orange_tulip",     Pair.of("minecraft:red_flower", 5))
        .put("minecraft:potted_white_tulip",      Pair.of("minecraft:red_flower", 6))
        .put("minecraft:potted_pink_tulip",       Pair.of("minecraft:red_flower", 7))
        .put("minecraft:potted_oxeye_daisy",      Pair.of("minecraft:red_flower", 8))
        .put("minecraft:potted_dandelion",        Pair.of("minecraft:yellow_flower", 0))
        .put("minecraft:potted_oak_sapling",      Pair.of("minecraft:sapling", 0))
        .put("minecraft:potted_spruce_sapling",   Pair.of("minecraft:sapling", 1))
        .put("minecraft:potted_birch_sapling",    Pair.of("minecraft:sapling", 2))
        .put("minecraft:potted_jungle_sapling",   Pair.of("minecraft:sapling", 3))
        .put("minecraft:potted_acacia_sapling",   Pair.of("minecraft:sapling", 4))
        .put("minecraft:potted_dark_oak_sapling", Pair.of("minecraft:sapling", 5))
        .put("minecraft:potted_brown_mushroom",   Pair.of("minecraft:brown_mushroom", 0))
        .put("minecraft:potted_red_mushroom",     Pair.of("minecraft:red_mushroom", 0))
        .put("minecraft:potted_dead_bush",        Pair.of("minecraft:deadbush", 0))
        .put("minecraft:potted_fern",             Pair.of("minecraft:tallgrass", 2))
        .put("minecraft:potted_cactus",           Pair.of("minecraft:cactus", 0))
        .build();

    static final String[] colorId = new String[] {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    static final Map<String, Integer> bedColor = new HashMap<>();
    static final Map<String, Integer> bannerColor = new HashMap<>();
    static {
        for (int i = 0; i < 16; i++)
        {
            bedColor.put("minecraft:" + colorId[i] + "_bed", i);
            // banner color ids are the metadata, which is reversed
            bannerColor.put("minecraft:" + colorId[i] + "_banner", 15 - i);
            bannerColor.put("minecraft:" + colorId[i] + "_wall_banner", 15 - i);
        }
    }

    static final UnfixBlockEntityCreator UNFIX_FLOWERPOT = (pos, container, blockEntityMap, originalTag) -> {
        Pair<String, Integer> itemData = flowerPotData.get(originalTag.getString("Name"));

        CompoundData flowerPotBeTag = new CompoundData();
        flowerPotBeTag.putString("Item", itemData.getLeft());
        flowerPotBeTag.putInt("Data", itemData.getRight());
        flowerPotBeTag.putString("id", "minecraft:flower_pot");
        flowerPotBeTag.putInt("x", pos.getX());
        flowerPotBeTag.putInt("y", pos.getY());
        flowerPotBeTag.putInt("z", pos.getZ());
        blockEntityMap.put(pos, flowerPotBeTag);
    };

    static final UnfixBlockEntityCreator UNFIX_NOTE_BLOCK = (pos, container, blockEntityMap, originalTag) -> {
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

    static final UnfixBlockEntityCreator UNFIX_BED = (pos, container, blockEntityMap, originalTag) -> {
        int color = bedColor.getOrDefault(originalTag.getString("Name"), 0);

        CompoundData blockEntityTag = new CompoundData();
        blockEntityTag.putInt("color", color);
        blockEntityTag.putString("id", "minecraft:bed");
        blockEntityTag.putInt("x", pos.getX());
        blockEntityTag.putInt("y", pos.getY());
        blockEntityTag.putInt("z", pos.getZ());
        blockEntityMap.put(pos, blockEntityTag);
    };

    // banner needs to get it's Base color id using the container
    // but the block entity should already exist in the schematic, recreate it to be safe
    static final UnfixBlockEntityCreator UNFIX_BANNER = (pos, container, blockEntityMap, originalTag) -> {
        CompoundData blockEntityTag = blockEntityMap.getOrDefault(pos, new CompoundData());
        // black by default
        blockEntityTag.putString("id", "minecraft:banner");

        // do not run unfixBannerBlockEntity(blockEntityTag) here, run it in the block entity converter

        int colorMetadata = bannerColor.getOrDefault(originalTag.getString("Name"), 15);
        blockEntityTag.putInt("Base", colorMetadata);
        blockEntityTag.putInt("x", pos.getX());
        blockEntityTag.putInt("y", pos.getY());
        blockEntityTag.putInt("z", pos.getZ());
        blockEntityMap.put(pos, blockEntityTag);
    };

    static void unfixBannerBlockEntity(CompoundData blockEntityTag)
    {
        if (blockEntityTag.containsList("Patterns", Constants.NBT.TAG_COMPOUND)) {
            ListData patterns = blockEntityTag.getList("Patterns", Constants.NBT.TAG_COMPOUND);
            if (patterns.size() > 0)
            {
                for (int i = 0; i < patterns.size(); i++)
                {
                    CompoundData pattern = patterns.getCompoundAt(i);
                    // 1.12 stores metadata color value, 1.13 stores id color value
                    pattern.putInt("Color", 15 - pattern.getIntOrDefault("Color", 0));
                }
            }
            else
            {
                blockEntityTag.remove("Patterns");
            }
        }
        else
        {
            blockEntityTag.remove("Patterns");
        }
    }

    static final Map<String, UnfixBlockEntityCreator> unfixers = new HashMap<>();
    static {
        unfixers.put("minecraft:flower_pot",              UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_poppy",            UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_blue_orchid",      UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_allium",           UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_azure_bluet",      UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_red_tulip",        UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_orange_tulip",     UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_white_tulip",      UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_pink_tulip",       UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_oxeye_daisy",      UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_dandelion",        UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_oak_sapling",      UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_spruce_sapling",   UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_birch_sapling",    UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_jungle_sapling",   UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_acacia_sapling",   UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_dark_oak_sapling", UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_brown_mushroom",   UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_red_mushroom",     UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_dead_bush",        UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_fern",             UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_cactus",           UNFIX_FLOWERPOT);
        unfixers.put("minecraft:note_block", UNFIX_NOTE_BLOCK);
        for (String color : colorId)
        {
            unfixers.put("minecraft:" + color + "_bed", UNFIX_BED);
            unfixers.put("minecraft:" + color + "_banner",      UNFIX_BANNER);
            unfixers.put("minecraft:" + color + "_wall_banner", UNFIX_BANNER);
        }
    }
}

interface UnfixBlockEntityCreator {
    void recreateBlockEntity(
        BlockPos pos,
        ArrayBlockContainer container,
        Map<BlockPos, CompoundData> blockEntityMap,
        CompoundData originalTag
    );
}
