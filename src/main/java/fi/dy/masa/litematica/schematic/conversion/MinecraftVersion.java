package fi.dy.masa.litematica.schematic.conversion;

import com.google.common.collect.ImmutableList;

public enum MinecraftVersion
{
    MC_1_12 ("1.12", 1139, 1343),
    MC_1_13 ("1.13", 1519, 1631),
    MC_1_14 ("1.14", 1952, 1976),
    MC_1_15 ("1.15", 2225, 2230);

    public static final ImmutableList<MinecraftVersion> KNOWN_VERSIONS = ImmutableList.copyOf(values());

    private final String versionName;
    private final int minSupportedMcDataVersion;
    private final int maxSupportedMcDataVersion;

    MinecraftVersion(String versionName, int minSupportedMcDataVersion, int maxSupportedMcDataVersion)
    {
        this.versionName = versionName;
        this.minSupportedMcDataVersion = minSupportedMcDataVersion;
        this.maxSupportedMcDataVersion = maxSupportedMcDataVersion;
    }

    public String getMcVersionDisplayName()
    {
        return this.versionName;
    }

    public int getMinDataVersion()
    {
        return this.minSupportedMcDataVersion;
    }

    public int getMaxDataVersion()
    {
        return this.maxSupportedMcDataVersion;
    }

    public boolean acceptsDataVersion(int dataVersion)
    {
        return dataVersion >= this.minSupportedMcDataVersion && dataVersion <= this.maxSupportedMcDataVersion;
    }

    public boolean isOlderThan(MinecraftVersion other)
    {
        return this.getMaxDataVersion() < other.getMinDataVersion();
    }

    private static int oldestKnownVersion;
    private static int latestKnownVersion;

    static
    {
        oldestKnownVersion = -1;
        latestKnownVersion = 0;

        for (MinecraftVersion version : MinecraftVersion.KNOWN_VERSIONS)
        {
            int dv = version.getMaxDataVersion();

            if (dv > latestKnownVersion)
            {
                latestKnownVersion = dv;
            }

            if (oldestKnownVersion == -1 || dv < oldestKnownVersion)
            {
                oldestKnownVersion = dv;
            }
        }
    }

    public static VersionClassification getVersionClassification(int dataVersion)
    {
        if (dataVersion < oldestKnownVersion)
        {
            return VersionClassification.OLD;
        }

        if (dataVersion > latestKnownVersion)
        {
            return VersionClassification.FUTURE;
        }

        return VersionClassification.KNOWN;
    }

    public enum VersionClassification
    {
        OLD,
        KNOWN,
        FUTURE;
    }
}
