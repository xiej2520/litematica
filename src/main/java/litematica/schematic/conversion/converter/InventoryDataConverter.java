package litematica.schematic.conversion.converter;

import malilib.util.game.MinecraftVersion;

import java.util.Optional;

import static malilib.util.game.MinecraftVersion.MC_1_12_2;

public class InventoryDataConverter extends DataConverterBase
{

    private InventoryDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        super(versionFrom, versionTo);
    }

    public static Optional<InventoryDataConverter> get(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        if (versionFrom.dataVersion > MC_1_12_2.dataVersion && versionTo.equals(MC_1_12_2))
        {
            return Optional.of(new InventoryDataConverter(versionFrom, versionTo));
        }
        return Optional.empty();
    }
}
