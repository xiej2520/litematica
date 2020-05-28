package fi.dy.masa.litematica.schematic.conversion.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;

public class BlockStateConverter extends DataConverterBase
{
    private final HashMap<NBTTagCompound, NBTTagCompound> stateMapping = new HashMap<>();

    public BlockStateConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        super(versionFrom, versionTo, "block_state_map.json", "block_states");
    }

    @Override
    protected void addMapping(JsonObject objFrom, JsonObject objTo)
    {
        NBTTagCompound tagFrom = blockStateJsonObjectToTag(objFrom);
        NBTTagCompound tagTo = blockStateJsonObjectToTag(objTo);

        if (tagFrom == null || tagTo == null)
        {
            InfoUtils.printErrorMessage("litematica.error.schematic_conversion.block_state_mapping.failed_to_read_mapping_from_json_data");
            return;
        }

        this.stateMapping.put(tagFrom, tagTo);
    }

    /**
     * Converts the provided block state palette.<br>
     * <b>Note:</b> The returned palette is independent of the input palette, all the tags are copied.
     * @param paletteTagIn
     * @return
     */
    public NBTTagList convertPalette(NBTTagList paletteTagIn)
    {
        final int paletteSize = paletteTagIn.tagCount();
        NBTTagList paletteTagOut = new NBTTagList();
        ArrayList<String> failedStates = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < paletteSize; ++i)
        {
            NBTTagCompound tag = paletteTagIn.getCompoundTagAt(i);
            NBTTagCompound convertedTag = this.stateMapping.get(tag);

            if (convertedTag != null)
            {
                //System.out.printf("converted: %s => %s\n", tag, convertedTag);
                paletteTagOut.appendTag(convertedTag.copy());
                ++successCount;
            }
            else
            {
                System.out.printf("FAILED: %s => %s\n", tag, convertedTag);
                failedStates.add(tag.toString());
                paletteTagOut.appendTag(tag.copy());
                ++failCount;
            }
        }

        if (failCount > 0)
        {
            String verFrom = this.versionFrom.getMcVersionDisplayName();
            String verTo = this.versionTo.getMcVersionDisplayName();
            String strSu = String.valueOf(successCount);
            String strFa = String.valueOf(failCount);
            InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, 8000, "litematica.message.warn.schematic_conversion.palette_conversion_failures", verFrom, verTo, strSu, strFa);
            InfoUtils.showGuiOrInGameMessage(MessageType.ERROR, 8000, String.join("\n", failedStates));
        }

        return paletteTagOut;
    }

    @Nullable
    public static NBTTagCompound blockStateJsonObjectToTag(JsonObject obj)
    {
        if (obj.has("block"))
        {
            NBTTagCompound stateTag = new NBTTagCompound();
            stateTag.setString("Name", obj.get("block").getAsString());

            JsonObject props = JsonUtils.getNestedObject(obj, "properties", false);

            if (props != null)
            {
                NBTTagCompound propsTag = new NBTTagCompound();

                for (Map.Entry<String, JsonElement> entry : props.entrySet())
                {
                    propsTag.setString(entry.getKey(), entry.getValue().getAsString());
                }

                stateTag.setTag("Properties", propsTag);
            }

            return stateTag;
        }

        return null;
    }
}
