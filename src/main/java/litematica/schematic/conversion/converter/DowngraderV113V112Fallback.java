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

public class DowngraderV113V112Fallback extends DowngraderV113V112
{
    public static DowngraderV113V112Fallback INSTANCE = new DowngraderV113V112Fallback();

    public static MinecraftVersion versionFrom = MinecraftVersion.MC_1_13;
    public static MinecraftVersion versionTo = MinecraftVersion.MC_1_12;

    private DowngraderV113V112Fallback()
    {
        super();
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

        this.itemMap.put("minecraft:pumpkin", new ItemIdDamage("minecraft:pumpkin", (short) 0));
    }


    // includes downgrades for 1.13 blockstates that don't have a 1.12 blockstate that converts to
    // the 1.13 one, but a somewhat reasonable replacement for them in 1.12 exists:
    //   mushroom blocks, <wood type> pressure plate/trapdoor/button, button/lever orientation,
    //   leaves[distance], purple shulker box, stripped wood/logs, void/cave air, pumpkin,
    //   waterlogged blocks, bubble column
    // remaining 1.13 blocks not converted:
    //  prismarine slab/stairs, prismarine brick slab/stairs, dark prismarine slab/stairs
    //  seagrass, tall seagrass, kelp, sea pickle, turtle egg, live/dead coral fan/blocks, conduit, blue ice
}

