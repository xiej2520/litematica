package litematica.schematic.conversion.converter;

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

import java.util.*;

public class DowngraderV113V112Fallback extends SchematicDataConverter
{
    public static DowngraderV113V112Fallback INSTANCE = new DowngraderV113V112Fallback();

    private Map<CompoundData, CompoundData> stateMap = new HashMap<>();

    public static MinecraftVersion versionFrom = MinecraftVersion.MC_1_13;
    public static MinecraftVersion versionTo = MinecraftVersion.MC_1_12;

    private DowngraderV113V112Fallback()
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

        Optional<Map<CompoundData, CompoundData>> fallbackStatemap = BlockStateMapReader.readMap("block_state_map_113_to_112_fallbacks.json", "1.13", "1.12");
        if (stateMap.isPresent())
        {
            for (Map.Entry<CompoundData, CompoundData> entry : fallbackStatemap.get().entrySet())
            {
                if (this.stateMap.containsKey(entry.getKey())) {
                    MessageDispatcher.error("conflicting entries for " + entry.getKey());
                    System.out.printf("conflicting entries for %s\n", entry);
                }
                else {
                    this.stateMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
        else
        {
            MessageDispatcher.error("failed to read block_state_map_113_to_112.json");
        }

        Map<CompoundData, CompoundData> waterlogged = new HashMap<>();
        for (Map.Entry<CompoundData, CompoundData> entry : this.stateMap.entrySet())
        {
            CompoundData props = entry.getKey().getCompound("Properties");
            if (props != null && props.contains("waterlogged", Constants.NBT.TAG_STRING)) {
                CompoundData waterloggedState = entry.getKey().copy();
                waterloggedState.getCompound("Properties").putString("waterlogged", "true");
                waterlogged.put(waterloggedState, entry.getValue());
                System.out.printf("WATERLOGGED MAPPING: %s %s %s\n",props, waterloggedState, entry.getValue());
            }
        }
        this.stateMap.putAll(waterlogged);
    }


    // includes downgrades for 1.13 blockstates that don't have a 1.12 blockstate that converts to
    // the 1.13 one, but a somewhat reasonable replacement for them in 1.12 exists:
    //   mushroom blocks, <wood type> pressure plate/trapdoor/button, button/lever orientation,
    //   leaves[distance], purple shulker box, stripped wood/logs, void/cave air, pumpkin,
    //   waterlogged blocks, bubble column
    // remaining 1.13 blocks not converted:
    //  prismarine slab/stairs, prismarine brick slab/stairs, dark prismarine slab/stairs
    //  seagrass, tall seagrass, kelp, sea pickle, turtle egg, live/dead coral fan/blocks, conduit, blue ice
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


}

