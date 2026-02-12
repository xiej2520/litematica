package litematica.schematic.conversion;

import litematica.schematic.container.ArrayBlockContainer;
import litematica.schematic.conversion.converter.*;
import litematica.schematic.data.EntityData;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.ListData;
import malilib.util.game.MinecraftVersion;
import malilib.util.position.BlockPos;
import malilib.util.world.ScheduledBlockTickData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public abstract class SchematicDataConverter {

    public void reset()
    {

    }

    private static Optional<SchematicDataConverter> getDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        if (versionTo.equals(MinecraftVersion.MC_1_12_2) && versionFrom.dataVersion > MinecraftVersion.MC_1_13.dataVersion)
        {
            return Optional.of(DowngraderV113V112.INSTANCE);
        }

        return Optional.empty();
    }

    // mutates input data
    public static void convert(
        ListData paletteTag,
        ArrayBlockContainer container,
        Map<BlockPos, CompoundData> blockEntityMap,
        Map<BlockPos, ScheduledBlockTickData> blockTickMap,
        List<EntityData> entityList,
        MinecraftVersion versionFrom,
        MinecraftVersion versionTo
    ) {
        Optional<SchematicDataConverter> converter = getDataConverter(versionFrom, versionTo);
        if (converter.isPresent()) {
            converter.get().convertContainer(paletteTag, container, blockEntityMap, blockTickMap);
            converter.get().convertEntityList(entityList);
        } else {
            MessageDispatcher.warning("failed to get converter from version " + versionFrom + " to " + versionTo);
        }
    }

    public abstract void convertContainer(
        ListData paletteTag,
        ArrayBlockContainer container,
        Map<BlockPos, CompoundData> blockEntityMap,
        Map<BlockPos, ScheduledBlockTickData> blockTickMap
    );

    public abstract void convertEntityList(List<EntityData> entityList);

}
