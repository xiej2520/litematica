package fi.dy.masa.litematica.schematic.conversion;

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
import org.apache.commons.io.IOUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.malilib.util.JsonUtils;

public interface IBlockStateMapReader
{
    /**
     * Returns the JSON file name to read the data from
     * @return
     */
    String getFileName();

    /**
     * Returns the JSON array name within the file
     * @return
     */
    String getArrayName();

    /**
     * Reads the conversion data from the provided JSON array
     * @param arr the JSON array that was read from the file
     * @return true if reading the data was successful
     */
    boolean read(JsonArray arr);

    default boolean read()
    {
        FileSystem filesystem = null;
        String fileName = this.getFileName();
        String location = "/assets/litematica/conversion_data/" + fileName;

        try
        {
            URL url = IBlockStateMapReader.class.getResource(location);

            if (url != null)
            {
                URI uri = url.toURI();
                Path path;

                if ("file".equals(uri.getScheme()))
                {
                    path = Paths.get(IBlockStateMapReader.class.getResource(location).toURI());
                }
                else
                {
                    if ("jar".equals(uri.getScheme()) == false)
                    {
                        Litematica.logger.error("Unsupported scheme " + uri + " while trying to read block state mapping data");
                        return false;
                    }

                    filesystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                    path = filesystem.getPath(location);
                }

                BufferedReader reader = Files.newBufferedReader(path);

                return this.read(reader);
            }
        }
        catch (Exception e)
        {
            Litematica.logger.error("Exception while trying to read block state mapping data", e);
        }
        finally
        {
            IOUtils.closeQuietly(filesystem);
        }

        return false;
    }

    default boolean read(BufferedReader reader) throws IOException
    {
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(reader);
        reader.close();

        if (element != null && element.isJsonObject())
        {
            JsonObject obj = element.getAsJsonObject();
            String arrayName = this.getArrayName();

            if (JsonUtils.hasArray(obj, arrayName))
            {
                return this.read(obj.get(arrayName).getAsJsonArray());
            }
        }

        return false;
    }
}
