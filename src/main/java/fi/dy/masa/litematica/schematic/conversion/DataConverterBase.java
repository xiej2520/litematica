package fi.dy.masa.litematica.schematic.conversion;

import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class DataConverterBase implements IBlockStateMapReader
{
    protected final MinecraftVersion versionFrom;
    protected final MinecraftVersion versionTo;
    protected final String fileName;
    protected final String arrayName;

    protected DataConverterBase(MinecraftVersion versionFrom, MinecraftVersion versionTo, String fileName, String arrayName)
    {
        this.versionFrom = versionFrom;
        this.versionTo = versionTo;
        this.fileName = fileName;
        this.arrayName = arrayName;
    }

    @Override
    public final String getFileName()
    {
        return this.fileName;
    }

    @Override
    public final String getArrayName()
    {
        return this.arrayName;
    }

    protected abstract void addMapping(JsonObject objFrom, JsonObject objTo);

    @Override
    public boolean read(JsonArray arr)
    {
        final int size = arr.size();

        for (int i = 0; i < size; ++i)
        {
            JsonObject obj = arr.get(i).getAsJsonObject();
            JsonObject objFrom = this.getNewestDataEntryForVersion(obj, this.versionFrom);
            JsonObject objTo = this.getNewestDataEntryForVersion(obj, this.versionTo);

            if (objFrom != null && objTo != null)
            {
                this.addMapping(objFrom, objTo);
            }
        }

        return true;
    }

    @Nullable
    protected JsonObject getNewestDataEntryForVersion(JsonObject mappingEntry, MinecraftVersion targetVersion)
    {
        String versionName = targetVersion.getVersionName();

        if (mappingEntry.has(versionName))
        {
            JsonElement el = mappingEntry.get(versionName);

            if (el.isJsonObject())
            {
                return el.getAsJsonObject();
            }
        }
        // Target version is not found directly, find the newest version that is older than the target version
        else
        {
            MinecraftVersion newestFound = null;

            for (MinecraftVersion version : MinecraftVersion.KNOWN_VERSIONS)
            {
                if (version.isOlderThan(targetVersion) &&
                    (newestFound == null || newestFound.isOlderThan(version)) &&
                    mappingEntry.has(version.getVersionName()))
                {
                    newestFound = version;
                }
            }

            if (newestFound != null)
            {
                versionName = newestFound.getVersionName();
                JsonElement el = mappingEntry.get(versionName);

                if (el.isJsonObject())
                {
                    return el.getAsJsonObject();
                }
            }
        }

        return null;
    }
}
