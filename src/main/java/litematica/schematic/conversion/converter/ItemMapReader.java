package litematica.schematic.conversion.converter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import litematica.Litematica;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.json.JsonUtils;
import malilib.util.data.tag.CompoundData;
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
        String location = "/assets/litematica/conversion_data/" + fileName;

        try (InputStream stream = BlockStateMapReader.class.getResourceAsStream(location))
        {
            if (stream == null)
            {
                Litematica.LOGGER.error("Resource not found: {}", location);
                return new ArrayList<>();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
            {
                try (Stream<String> lines = reader.lines()) {
                    return lines.map(String::trim)
                        .filter(l -> !l.isEmpty())
                        .map(l -> JsonParser.parseString(l).getAsJsonObject())
                        .collect(Collectors.toList());
                }
            }
        }
        catch (Exception e)
        {
            Litematica.LOGGER.error("Exception while trying to read item mapping data", e);
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
