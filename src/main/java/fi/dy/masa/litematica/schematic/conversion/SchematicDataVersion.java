package fi.dy.masa.litematica.schematic.conversion;

import javax.annotation.Nullable;

public class SchematicDataVersion
{
    private final MinecraftVersion mcVersion;
    private final int dataVersion;

    private SchematicDataVersion(MinecraftVersion mcVersion, int dataVersion)
    {
        this.mcVersion = mcVersion;
        this.dataVersion = dataVersion;
    }

    public MinecraftVersion getMinecraftVersion()
    {
        return this.mcVersion;
    }

    public int getDataVersion()
    {
        return this.dataVersion;
    }

    public String getMcVersionDisplayName()
    {
        return this.mcVersion.getMcVersionDisplayName();
    }

    @Nullable
    public static SchematicDataVersion getVersionFor(int dataVersion)
    {
        for (MinecraftVersion version : MinecraftVersion.KNOWN_VERSIONS)
        {
            if (version.acceptsDataVersion(dataVersion))
            {
                return new SchematicDataVersion(version, dataVersion);
            }
        }

        return null;
    }
}
