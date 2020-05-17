package fi.dy.masa.litematica.schematic.conversion;

import java.util.HashMap;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

public class SchematicDataConversionManager
{
    public static final SchematicDataConversionManager INSTANCE = new SchematicDataConversionManager();

    private final HashMap<Pair<MinecraftVersion, MinecraftVersion>, BlockStateConverter> blockStateConverters = new HashMap<>();

    private SchematicDataConversionManager()
    {
    }

    public void reset()
    {
        this.blockStateConverters.clear();
    }

    @Nullable
    public BlockStateConverter getBlockStateConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        Pair<MinecraftVersion, MinecraftVersion> key = Pair.of(versionFrom, versionTo);
        BlockStateConverter converter = this.blockStateConverters.get(key);

        if (converter == null)
        {
            BlockStateConverter converterTmp = new BlockStateConverter(versionFrom, versionTo);

            if (converterTmp.read())
            {
                converter = converterTmp;
                this.blockStateConverters.put(key, converterTmp);
            }
        }

        return converter;
    }
}
