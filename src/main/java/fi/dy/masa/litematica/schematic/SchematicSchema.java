package fi.dy.masa.litematica.schematic;

public class SchematicSchema
{
    private final int litematicVersion;
    private final int minecraftDataVersion;

    public SchematicSchema(int litematicVersion, int minecraftDataVersion)
    {
        this.litematicVersion = litematicVersion;
        this.minecraftDataVersion = minecraftDataVersion;
    }

    public int litematicVersion()
    {
        return this.litematicVersion;
    }

    public int minecraftDataVersion()
    {
        return this.minecraftDataVersion;
    }

    @Override
    public String toString()
    {
        return "V" + this.litematicVersion() + " / DataVersion " + this.minecraftDataVersion();
    }
}
