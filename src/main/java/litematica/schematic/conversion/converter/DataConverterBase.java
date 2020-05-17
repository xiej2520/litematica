package litematica.schematic.conversion.converter;

import malilib.util.game.MinecraftVersion;

public class DataConverterBase
{

    protected final MinecraftVersion versionFrom;
    protected final MinecraftVersion versionTo;

    protected DataConverterBase(MinecraftVersion versionFrom, MinecraftVersion versionTo) {
        this.versionFrom = versionFrom;
        this.versionTo = versionTo;
    }
}
