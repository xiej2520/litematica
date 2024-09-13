package fi.dy.masa.litematica.util;

import fi.dy.masa.litematica.schematic.LitematicaSchematic;

// <ai>Only the Minecraft data version table of upstream's DataFixerMode; this branch has no vanilla data fixer modes.</ai>
public class DataFixerMode
{
    public static Schema getSchemaByVersion(int dataVersion)
    {
        for (Schema schema : Schema.values())
        {
            if (schema.getDataVersion() <= dataVersion)
            {
                return schema;
            }
        }

        return getSchemaByVersion(LitematicaSchematic.MINECRAFT_DATA_VERSION_1_12);
    }

    public enum Schema
    {
        // Minecraft Data Versions
        SCHEMA_24W37A  (4065, "24w37a"),
        SCHEMA_1_21_01 (3955, "1.21.1"),
        SCHEMA_1_21_00 (3953, "1.21"),
        SCHEMA_1_20_05 (3837, "1.20.5"),
        SCHEMA_24W10A  (3821, "24w10a"),
        SCHEMA_24W09A  (3819, "24w09a"), // Data Components ( https://minecraft.wiki/w/Data_component_format )
        SCHEMA_24W07A  (3817, "24w07a"),
        SCHEMA_1_20_04 (3700, "1.20.4"),
        SCHEMA_1_20_02 (3578, "1.20.2"),
        SCHEMA_1_20_01 (3465, "1.20.1"),
        SCHEMA_1_20_00 (3463, "1.20"),
        SCHEMA_1_19_04 (3337, "1.19.4"),
        SCHEMA_1_19_00 (3105, "1.19"),
        SCHEMA_1_18_02 (2975, "1.18.2"),
        SCHEMA_1_18_00 (2860, "1.18"),
        SCHEMA_1_17_01 (2730, "1.17.1"),
        SCHEMA_1_17_00 (2724, "1.17"),
        SCHEMA_1_16_05 (2586, "1.16.5"),
        SCHEMA_1_16_00 (2566, "1.16"),
        SCHEMA_1_15_02 (2230, "1.15.2"),
        SCHEMA_1_15_00 (2225, "1.15"),
        SCHEMA_1_14_04 (1976, "1.14.4"),
        SCHEMA_1_14_00 (1952, "1.14"),
        SCHEMA_1_13_02 (1631, "1.13.2"),
        SCHEMA_1_13_00 (1519, "1.13"),
        SCHEMA_17W47A  (1451, "17w47a"), // The Flattening ( https://minecraft.wiki/w/Java_Edition_1.13/Flattening )
        SCHEMA_17W46A  (1449, "17w46a"),
        SCHEMA_1_12_02 (1343, "1.12.2"),
        SCHEMA_1_12_00 (1139, "1.12"),
        SCHEMA_1_11_02 (922,  "1.11.2"),
        SCHEMA_1_11_00 (819,  "1.11"),
        SCHEMA_1_10_02 (512,  "1.10.2"),
        SCHEMA_1_10_00 (510,  "1.10"),
        SCHEMA_1_09_04 (184,  "1.9.4"),
        SCHEMA_1_09_00 (169,  "1.9"),
        SCHEMA_15W32A  (100,  "15w32a");

        private final int schemaId;
        private final String str;

        Schema(int id, String ver)
        {
            this.schemaId = id;
            this.str = ver;
        }

        public int getDataVersion()
        {
            return this.schemaId;
        }

        public String getString()
        {
            return this.str;
        }

        @Override
        public String toString()
        {
            return "MC: "+this.getString()+" [Schema: "+this.getDataVersion()+"]";
        }
    }
}
