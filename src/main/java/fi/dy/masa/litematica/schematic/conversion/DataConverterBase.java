package fi.dy.masa.litematica.schematic.conversion;

import javax.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class DataConverterBase implements IBlockStateMapReader
{
    protected final MinecraftVersion versionFrom;
    protected final MinecraftVersion versionTo;

    protected DataConverterBase(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        this.versionFrom = versionFrom;
        this.versionTo = versionTo;
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
    protected JsonObject getNewestDataEntryForVersion(JsonObject mappingEntry, MinecraftVersion version)
    {
        String versionName = version.getMcVersionDisplayName();

        if (mappingEntry.has(versionName))
        {
            JsonElement el = mappingEntry.get(versionName);

            if (el.isJsonObject())
            {
                return el.getAsJsonObject();
            }
        }
        else
        {
            MinecraftVersion newestFound = null;

            for (MinecraftVersion v : MinecraftVersion.KNOWN_VERSIONS)
            {
                if (v.isOlderThan(version) &&
                    (newestFound == null || newestFound.isOlderThan(v)) &&
                    mappingEntry.has(v.getMcVersionDisplayName()))
                {
                    newestFound = v;
                }
            }

            if (newestFound != null)
            {
                versionName = newestFound.getMcVersionDisplayName();
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
