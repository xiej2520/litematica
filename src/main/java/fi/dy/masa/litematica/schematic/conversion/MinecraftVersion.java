package fi.dy.masa.litematica.schematic.conversion;

import com.google.common.collect.ImmutableList;

public enum MinecraftVersion
{
    // We should only have versions here that have some differing
    // data that needs to be converted to from any other version
    /*
    MC_1_10_X ("1.12", "1.10.x",  510,  512),
    MC_1_11_X ("1.12", "1.11.x",  819,  922),
    MC_1_12_0 ("1.12", "1.12.0", 1139, 1139),
    MC_1_12_1 ("1.12", "1.12.1", 1241, 1241),
    MC_1_12_2 ("1.12", "1.12.2", 1343, 1343),
    MC_1_13_0 ("1.13", "1.13.0", 1519, 1519),
    MC_1_13_1 ("1.13", "1.13.1", 1628, 1628),
    MC_1_13_2 ("1.13", "1.13.2", 1631, 1631),
    MC_1_14_0 ("1.14", "1.14.0", 1952, 1952),
    MC_1_14_1 ("1.14", "1.14.1", 1957, 1957),
    MC_1_14_2 ("1.14", "1.14.2", 1963, 1963),
    MC_1_14_3 ("1.14", "1.14.3", 1968, 1968),
    MC_1_14_4 ("1.14", "1.14.4", 1976, 1976),
    MC_1_15_0 ("1.15", "1.15.0", 2225, 2225),
    MC_1_15_1 ("1.15", "1.15.1", 2227, 2227),
    MC_1_15_2 ("1.15", "1.15.2", 2230, 2230);
    */

    MC_1_12_X ("1.12", "1.12.x", 1139, 1343, 4),
    MC_1_13_X ("1.13", "1.13.x", 1519, 1631, 5),
    MC_1_14_X ("1.14", "1.14.x", 1952, 1976, 5),
    MC_1_15_X ("1.15", "1.15.x", 2225, 2230, 5);

    public static final ImmutableList<MinecraftVersion> KNOWN_VERSIONS = ImmutableList.copyOf(values());

    private final String mappingDataVersionName;
    private final String versionDisplayName;
    private final int minSupportedMcDataVersion;
    private final int maxSupportedMcDataVersion;
    private final int schematicVersion;

    MinecraftVersion(String mappingDataVersionName, String versionDisplayName, int minSupportedMcDataVersion, int maxSupportedMcDataVersion, int schematicVersion)
    {
        this.mappingDataVersionName = mappingDataVersionName;
        this.versionDisplayName = versionDisplayName;
        this.minSupportedMcDataVersion = minSupportedMcDataVersion;
        this.maxSupportedMcDataVersion = maxSupportedMcDataVersion;
        this.schematicVersion = schematicVersion;
    }

    public String getVersionName()
    {
        return mappingDataVersionName;
    }

    public String getMcVersionDisplayName()
    {
        return this.versionDisplayName;
    }

    public int getSchematicVersion()
    {
        return this.schematicVersion;
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

    public static MinecraftVersion getOldestKnownVersion()
    {
        return KNOWN_VERSIONS.get(0);
    }

    public static MinecraftVersion getLatestKnownVersion()
    {
        return KNOWN_VERSIONS.get(KNOWN_VERSIONS.size() - 1);
    }

    public enum VersionClassification
    {
        OLD,
        KNOWN,
        FUTURE;
    }
}
