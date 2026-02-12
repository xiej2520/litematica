package litematica.schematic.conversion.converter;

import java.io.BufferedReader;
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
import org.apache.commons.io.IOUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BlockStateMapReader
{
    static Optional<Map<CompoundData, CompoundData>> readMap(String fileName, String versionFrom, String versionTo)
    {
        Optional<JsonArray> arrOpt = read(fileName);
        if (arrOpt.isPresent() == false)
        {
            return Optional.empty();
        }
        JsonArray arr = arrOpt.get();

        HashMap<CompoundData, CompoundData> map = new HashMap<>();
        final int size = arr.size();

        for (int i = 0; i < size; ++i)
        {
            try {
                JsonObject obj = arr.get(i).getAsJsonObject();
                JsonObject objFrom = obj.get(versionFrom).getAsJsonObject();
                JsonObject objTo = obj.get(versionTo).getAsJsonObject();

                if (objFrom != null && objTo != null) {
                    Optional<CompoundData> tagFrom = blockStateJsonObjectToTag(objFrom);
                    Optional<CompoundData> tagTo = blockStateJsonObjectToTag(objTo);

                    System.out.printf("MAPPING: %s => %s\n", tagFrom, tagTo);
                    if (tagFrom.isPresent() && tagTo.isPresent()) {
                        map.put(tagFrom.get(), tagTo.get());
                    } else {
                        MessageDispatcher.error("litematica.error.schematic_conversion.block_state_mapping.failed_to_read_mapping_from_json_data");
                    }
                }
            } catch (Exception e) {
                MessageDispatcher.error("litematica.error.schematic_conversion.block_state_mapping.failed_to_read_mapping_from_json_data", e);
            }
        }

        return Optional.of(map);
    }


    public static Optional<JsonArray> read(String fileName)
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

                JsonElement element = JsonParser.parseReader(reader);
                reader.close();

                if (element != null && element.isJsonObject())
                {
                    JsonObject obj = element.getAsJsonObject();
                    if (JsonUtils.hasArray(obj, "block_states"))
                    {
                        return Optional.of(obj.get("block_states").getAsJsonArray());
                    }
                }
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

}
