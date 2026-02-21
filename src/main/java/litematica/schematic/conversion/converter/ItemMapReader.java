package litematica.schematic.conversion.converter;

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import litematica.Litematica;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.json.JsonUtils;
import malilib.util.data.tag.CompoundData;
import org.apache.commons.io.IOUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ItemMapReader
{
    static Optional<Map<CompoundData, CompoundData>> readMap(String fileName, String versionFrom, String versionTo)
    {
        List<JsonObject> arr = readNdjson(fileName);
        if (arr.isEmpty())
        {
            return Optional.empty();
        }

        HashMap<CompoundData, CompoundData> map = new HashMap<>();

        for (JsonObject jsonObject : arr) {
            try {
                JsonObject obj = jsonObject.getAsJsonObject();
                JsonObject objFrom = obj.get(versionFrom).getAsJsonObject();
                JsonObject objTo = obj.get(versionTo).getAsJsonObject();

                if (objFrom != null && objTo != null) {
                    Optional<CompoundData> tagFrom = itemJsonObjectToTag(objFrom);
                    Optional<CompoundData> tagTo = itemJsonObjectToTag(objTo);

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


    public static List<JsonObject> readNdjson(String fileName)
    {
        FileSystem filesystem = null;
        String location = "/assets/litematica/conversion_data/" + fileName;

        try
        {
            URL url = ItemMapReader.class.getResource(location);
            if (url == null) {
                return new ArrayList<>();
            }

            URI uri = url.toURI();
            Path path;

            if ("file".equals(uri.getScheme()))
            {
                path = Paths.get(uri);
            }
            else if ("jar".equals(uri.getScheme()))
            {
                filesystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                path = filesystem.getPath(location);
            }
            else
            {
                Litematica.LOGGER.error("Unsupported scheme " + uri + " while trying to read item mapping data");
                return new ArrayList<>();
            }

            try (Stream<String> lines = Files.lines(path)) {
                return lines.map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .map(l -> JsonParser.parseString(l).getAsJsonObject())
                    .collect(Collectors.toList());
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

        return new ArrayList<>();
    }

    public static Optional<CompoundData> itemJsonObjectToTag(JsonObject obj)
    {
        JsonElement id = obj.get("id");
        if (id != null)
        {
            CompoundData itemTag = new CompoundData();
            itemTag.putString("id", id.getAsString());

            JsonUtils.getIntegerIfExists(obj, "Damage", damage -> itemTag.putShort("Damage", (short) damage));

            // ignore "tag", handle conversion separately

            return Optional.of(itemTag);
        }

        return Optional.empty();
    }

}
