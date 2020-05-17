package litematica.schematic.conversion.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import litematica.Litematica;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.json.JsonUtils;
import malilib.util.data.tag.CompoundData;
import malilib.util.game.MinecraftVersion;
import org.apache.commons.io.IOUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BlockStateMapReader
{
    public static Optional<CompoundData> blockStateJsonObjectToTag(JsonObject obj)
    {
        JsonElement block = obj.get("block");
        if (block != null)
        {
            CompoundData stateTag = new CompoundData();
            stateTag.putString("Name", block.getAsString());

            JsonObject props = JsonUtils.getNestedObject(obj, "properties", false);

            if (props != null)
            {
                CompoundData propsTag = new CompoundData();

                for (Map.Entry<String, JsonElement> entry : props.entrySet())
                {
                    propsTag.putString(entry.getKey(), entry.getValue().getAsString());
                }

                stateTag.put("Properties", propsTag);
            }

            return Optional.of(stateTag);
        }

        return Optional.empty();
    }

    protected static Optional<JsonObject> getNewestDataEntryForVersion(JsonObject mappingEntry, MinecraftVersion targetVersion)
    {
        String versionName = targetVersion.displayName;

        if (mappingEntry.has(versionName))
        {
            JsonElement el = mappingEntry.get(versionName);

            if (el.isJsonObject())
            {
                return Optional.ofNullable(el.getAsJsonObject());
            }
        }
        // Target version is not found directly, find the newest version that is older than the target version
        else
        {
            MinecraftVersion newestFound = null;

            for (MinecraftVersion version : MinecraftVersion.MINECRAFT_RELEASE_VERSIONS)
            {
                if (version.dataVersion < targetVersion.dataVersion
                    && (newestFound == null || newestFound.dataVersion < version.dataVersion)
                    && mappingEntry.has(version.displayName))
                {
                    newestFound = version;
                }
            }

            if (newestFound != null)
            {
                versionName = newestFound.displayName;
                JsonElement el = mappingEntry.get(versionName);

                if (el.isJsonObject())
                {
                    return Optional.ofNullable(el.getAsJsonObject());
                }
            }
        }

        return Optional.empty();
    }

    static HashMap<CompoundData, CompoundData> read(JsonArray arr, MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        HashMap<CompoundData, CompoundData> map = new HashMap<>();
        final int size = arr.size();

        for (int i = 0; i < size; ++i)
        {
            JsonObject obj = arr.get(i).getAsJsonObject();
            Optional<JsonObject> objFrom = getNewestDataEntryForVersion(obj, versionFrom);
            Optional<JsonObject> objTo = getNewestDataEntryForVersion(obj, versionTo);

            if (objFrom.isPresent() && objTo.isPresent())
            {
                Optional<CompoundData> tagFrom = blockStateJsonObjectToTag(objFrom.get());
                Optional<CompoundData> tagTo = blockStateJsonObjectToTag(objTo.get());

                System.out.printf("MAPPING: %s => %s\n", tagFrom, tagTo);
                if (tagFrom.isPresent() && tagTo.isPresent())
                {
                    map.put(tagFrom.get(), tagTo.get());
                }
                else
                {
                    MessageDispatcher.error("litematica.error.schematic_conversion.block_state_mapping.failed_to_read_mapping_from_json_data");
                }
            }
        }

        return map;
    }

    static Optional<JsonArray> read(String fileName, String arrayName)
    {
        FileSystem filesystem = null;
        String location = "/assets/litematica/conversion_data/" + fileName;

        try
        {
            URL url = BlockStateMapReader.class.getResource(location);

            if (url != null)
            {
                URI uri = url.toURI();
                Path path;

                if ("file".equals(uri.getScheme()))
                {
                    path = Paths.get(BlockStateMapReader.class.getResource(location).toURI());
                }
                else
                {
                    if ("jar".equals(uri.getScheme()) == false)
                    {
                        Litematica.LOGGER.error("Unsupported scheme " + uri + " while trying to read block state mapping data");
                        return Optional.empty();
                    }

                    filesystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                    path = filesystem.getPath(location);
                }

                BufferedReader reader = Files.newBufferedReader(path);

                return read(reader, arrayName);
            }
        }
        catch (Exception e)
        {
            Litematica.LOGGER.error("Exception while trying to read block state mapping data", e);
        }
        finally
        {
            IOUtils.closeQuietly(filesystem);
        }

        return Optional.empty();
    }

    static Optional<JsonArray> read(BufferedReader reader, String arrayName) throws IOException
    {
        JsonElement element = JsonParser.parseReader(reader);
        reader.close();

        if (element != null && element.isJsonObject())
        {
            JsonObject obj = element.getAsJsonObject();

            if (JsonUtils.hasArray(obj, arrayName))
            {
                return Optional.ofNullable(obj.get(arrayName).getAsJsonArray());
            }
        }

        return Optional.empty();
    }
}
