package litematica.schematic.conversion;

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

public class SchematicConverter {

    public static final SchematicConverter INSTANCE = new SchematicConverter();

    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, BlockStateConverter> blockStateConverters = new HashMap<>();
    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, BlockEntityDataConverter> blockEntityDataConverters = new HashMap<>();
    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, BlockTickDataConverter> blockTickDataConverters = new HashMap<>();
    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, EntityDataConverter> entityDataConverters = new HashMap<>();
    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, InventoryDataConverter> inventoryDataConverters = new HashMap<>();

    private SchematicConverter()
    {

    }

    public void reset()
    {
        this.blockStateConverters.clear();
        this.blockEntityDataConverters.clear();
        this.blockTickDataConverters.clear();
        this.entityDataConverters.clear();
        this.inventoryDataConverters.clear();
    }

    public Optional<BlockStateConverter> getBlockStateConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        return this.getDataConverter(versionFrom, versionTo, BlockStateConverter::get, this.blockStateConverters);
    }

    public Optional<BlockEntityDataConverter> getBlockEntityDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        return this.getDataConverter(versionFrom, versionTo, BlockEntityDataConverter::get, this.blockEntityDataConverters);
    }

    public Optional<BlockTickDataConverter> getBlockTickDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        return this.getDataConverter(versionFrom, versionTo, BlockTickDataConverter::get, this.blockTickDataConverters);
    }

    public Optional<EntityDataConverter> getEntityDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        return this.getDataConverter(versionFrom, versionTo, EntityDataConverter::get, this.entityDataConverters);
    }

    public Optional<InventoryDataConverter> getInventoryDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        return this.getDataConverter(versionFrom, versionTo, InventoryDataConverter::get, this.inventoryDataConverters);
    }

    private <T extends DataConverterBase> Optional<T> getDataConverter(
        MinecraftVersion versionFrom,
        MinecraftVersion versionTo,
        BiFunction<MinecraftVersion, MinecraftVersion, Optional<T>> factory,
        Map<Pair<MinecraftVersion, MinecraftVersion>, T> map)
    {
        Pair<MinecraftVersion, MinecraftVersion> key = Pair.of(versionFrom, versionTo);
        T converter = map.get(key);

        if (converter == null)
        {
            Optional<T> converterOpt = factory.apply(versionFrom, versionTo);
            converterOpt.ifPresent(c -> map.put(key, c));
            return converterOpt;
        }

        return Optional.of(converter);
    }

    public static ListData convertBlockStatePalette(ListData paletteTag, MinecraftVersion versionFrom, MinecraftVersion versionTo) {
        Optional<BlockStateConverter> converter = INSTANCE.getBlockStateConverter(versionFrom, versionTo);
        if (converter.isPresent())
        {
            return converter.get().convertedPalette(paletteTag);
        }
        else
        {
            MessageDispatcher.error("litematica.error.schematic.conversion.missing_converter.block_states",
                versionFrom.displayName, versionTo.displayName);
            return paletteTag;
        }
    }

    public static void convertBlockEntityMap(Map<BlockPos, CompoundData> blockEntityMap, MinecraftVersion versionFrom, MinecraftVersion versionTo) {

    }

    public static void convertBlockTickMap(Map<BlockPos, ScheduledBlockTickData> blockTickMap, MinecraftVersion versionFrom, MinecraftVersion versionTo) {


    }

    public static void convertEntityList(List<EntityData> entityList, MinecraftVersion versionFrom, MinecraftVersion versionTo) {

    }

}
