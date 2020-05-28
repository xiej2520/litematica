package fi.dy.masa.litematica.schematic.conversion.converter;

import java.util.HashMap;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.malilib.gui.util.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;

public abstract class EntityDataConverterBase extends DataConverterBase
{
    private final HashMap<String, String> nameMapping = new HashMap<>();
    protected final String errorMessageAddMapping;
    protected final String infoMessageConverted;

    protected EntityDataConverterBase(MinecraftVersion versionFrom, MinecraftVersion versionTo, String errorMessageAddMapping, String infoMessageConverted)
    {
        super(versionFrom, versionTo, "entity_map.json", "entities");

        this.errorMessageAddMapping = errorMessageAddMapping;
        this.infoMessageConverted = infoMessageConverted;
    }

    @Override
    protected void addMapping(JsonObject objFrom, JsonObject objTo)
    {
        if (JsonUtils.hasString(objFrom, "name") == false || JsonUtils.hasString(objTo, "name") == false)
        {
            InfoUtils.printErrorMessage(this.errorMessageAddMapping);
            return;
        }

        String nameFrom = JsonUtils.getString(objFrom, "name");
        String nameTo = JsonUtils.getString(objTo, "name");

        this.nameMapping.put(nameFrom, nameTo);
    }

    public void convertName(NBTTagCompound tag, String tagName)
    {
        String nameIn = tag.getString(tagName);
        String newName = this.nameMapping.get(nameIn);

        if (newName != null && newName.equals(nameIn) == false)
        {
            tag.setString(tagName, newName);
        }
    }

    public NBTTagList convertEntityNames(NBTTagList entityListIn, String idTagName)
    {
        NBTTagList entityListOut = new NBTTagList();
        final int count = entityListIn.tagCount();
        int successCount = 0;
        int keepCount = 0;

        for (int i = 0; i < count; ++i)
        {
            NBTTagCompound tag = entityListIn.getCompoundTagAt(i);
            String oldName = tag.getString(idTagName);
            String newName = this.nameMapping.get(oldName);

            if (newName == null && newName.equals(oldName) == false)
            {
                System.out.printf("renamed: %s => %s\n", oldName, newName);
                tag.setString(idTagName, newName);
                ++successCount;
            }
            else
            {
                ++keepCount;
            }

            entityListOut.appendTag(tag);
        }

        String strConv = String.valueOf(successCount);
        String strKeep = String.valueOf(keepCount);
        InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, 8000, this.infoMessageConverted, strConv, strKeep);

        return entityListOut;
    }
}
