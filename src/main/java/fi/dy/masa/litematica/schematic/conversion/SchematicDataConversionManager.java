package fi.dy.masa.litematica.schematic.conversion;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

public class SchematicDataConversionManager
{
    public static final SchematicDataConversionManager INSTANCE = new SchematicDataConversionManager();

    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, BlockStateConverter> blockStateConverters = new HashMap<>();
    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, BlockEntityDataConverter> blockEntityDataConverters = new HashMap<>();
    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, BlockTickDataConverter> blockTickDataConverters = new HashMap<>();
    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, EntityDataConverter> entityDataConverters = new HashMap<>();
    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, InventoryDataConverter> inventoryDataConverters = new HashMap<>();

    private SchematicDataConversionManager()
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

    @Nullable
    public BlockStateConverter getBlockStateConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        return this.getDataConverter(versionFrom, versionTo, BlockStateConverter::new, this.blockStateConverters);
    }

    @Nullable
    public BlockEntityDataConverter getBlockEntityDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        return this.getDataConverter(versionFrom, versionTo, BlockEntityDataConverter::new, this.blockEntityDataConverters);
    }

    @Nullable
    public BlockTickDataConverter getBlockTickDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        return this.getDataConverter(versionFrom, versionTo, BlockTickDataConverter::new, this.blockTickDataConverters);
    }

    @Nullable
    public EntityDataConverter getEntityDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        return this.getDataConverter(versionFrom, versionTo, EntityDataConverter::new, this.entityDataConverters);
    }

    @Nullable
    public InventoryDataConverter getInventoryDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        return this.getDataConverter(versionFrom, versionTo, InventoryDataConverter::new, this.inventoryDataConverters);
    }

    @Nullable
    private <T extends DataConverterBase> T getDataConverter(MinecraftVersion versionFrom,
                                                             MinecraftVersion versionTo,
                                                             BiFunction<MinecraftVersion, MinecraftVersion, T> factory,
                                                             Map<Pair<MinecraftVersion, MinecraftVersion>, T> map)
    {
        Pair<MinecraftVersion, MinecraftVersion> key = Pair.of(versionFrom, versionTo);
        T converter = map.get(key);

        if (converter == null)
        {
            T converterTmp = factory.apply(versionFrom, versionTo);

            if (converterTmp.read())
            {
                converter = converterTmp;
                map.put(key, converterTmp);
            }
        }

        return converter;
    }
}
