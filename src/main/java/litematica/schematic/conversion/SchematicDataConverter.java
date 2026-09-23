package litematica.schematic.conversion;

import litematica.schematic.LitematicaSchematic;
import litematica.schematic.Schematic;
import litematica.schematic.container.ArrayBlockContainer;
import litematica.schematic.conversion.converter.*;
import litematica.schematic.data.EntityData;
import malilib.gui.BaseScreen;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.ListData;
import malilib.util.game.MinecraftVersion;
import malilib.util.position.BlockPos;
import malilib.util.world.ScheduledBlockTickData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class SchematicDataConverter {

    public void reset()
    {

    }

    private static Optional<SchematicDataConverter> getDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        if (versionTo.equals(MinecraftVersion.MC_1_12_2) && versionFrom.dataVersion >= MinecraftVersion.MC_1_13.dataVersion)
        {
            //return Optional.of(DowngraderV113V112Fallback.INSTANCE);
            return Optional.of(DowngraderV113V112.INSTANCE);
        }

        return Optional.empty();
    }

    // mutates input data
    public static ConversionResult convert(
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
            ConversionResult result = converter.get().convertContainer(paletteTag, container, blockEntityMap, blockTickMap);
            converter.get().convertEntityList(entityList);

            return result;
        } else {
            MessageDispatcher.warning("failed to get converter from version " + versionFrom + " to " + versionTo);
        }
        return ConversionResult.empty();
    }

    public static void reportConversionResult(ConversionResult result)
    {
        if (result.hasFailures())
        {
            MessageDispatcher.warning("litematica.message.warn.schematic_conversion.palette_conversion_failures",
                                      String.valueOf(result.successCount), String.valueOf(result.failedStates.size()));
            MessageDispatcher.error(String.join("\n", result.failedStates));
        }
    }

    public static void showFailureScreen(ConversionResult result)
    {
        if (result.failedStates.isEmpty() == false)
        {
            BaseScreen.openPopupScreenWithCurrentScreenAsParent(new SaveConversionFailureLogScreen(result.failedStates));
        }
    }

    public static final class ConversionResult
    {
        public final int successCount;
        public final List<String> failedStates;

        public ConversionResult(int successCount, List<String> failedStates)
        {
            this.successCount = successCount;
            this.failedStates = Collections.unmodifiableList(new ArrayList<>(failedStates));
        }

        public static ConversionResult empty()
        {
            return new ConversionResult(0, Collections.emptyList());
        }

        public boolean hasFailures()
        {
            return this.failedStates.isEmpty() == false;
        }
    }

    public abstract ConversionResult convertContainer(
        ListData paletteTag,
        ArrayBlockContainer container,
        Map<BlockPos, CompoundData> blockEntityMap,
        Map<BlockPos, ScheduledBlockTickData> blockTickMap
    );

    public abstract void convertEntityList(List<EntityData> entityList);

}
