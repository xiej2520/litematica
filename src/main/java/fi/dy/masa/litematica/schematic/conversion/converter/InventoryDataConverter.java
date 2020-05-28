package fi.dy.masa.litematica.schematic.conversion.converter;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import fi.dy.masa.litematica.schematic.conversion.ItemIdentity;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;

public class InventoryDataConverter extends DataConverterBase
{
    protected final ArrayListMultimap<String, ArrayList<String>> inventoryPaths = ArrayListMultimap.create();
    protected final HashBiMap<ItemIdentity, ItemIdentity> itemConversions = HashBiMap.create();
    protected ItemIdentity temporaryIdentity = new ItemIdentity();

    public InventoryDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        super(versionFrom, versionTo, "item_map.json", "items");
    }

    @Override
    protected void addMapping(JsonObject objFrom, JsonObject objTo)
    {
        ItemIdentity identityFrom = ItemIdentity.fromJson(objFrom);
        ItemIdentity identityTo = ItemIdentity.fromJson(objTo);

        if (identityFrom != null && identityTo != null)
        {
            this.itemConversions.put(identityFrom, identityTo);
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.error.schematic_conversion.item_mapping.failed_to_read_data_from_json");
        }
    }

    public void convertAnyInventoryContentsInList(NBTTagList entityListIn, String idTagName)
    {
        final int count = entityListIn.tagCount();

        for (int i = 0; i < count; ++i)
        {
            NBTTagCompound tag = entityListIn.getCompoundTagAt(i);
            this.convertAnyInventoryContents(tag.getString(idTagName), tag);
        }
    }

    /**
     * Converts any supported inventory tags.<br>
     * <b>Note:</b> The conversion is done in place, so the given input tag must be safely modifiable!
     * @param ownerId
     * @param tagIn
     */
    public void convertAnyInventoryContents(String ownerId, NBTTagCompound tagIn)
    {
        List<ArrayList<String>> dataPaths = this.inventoryPaths.get(ownerId);

        if (dataPaths.isEmpty() == false)
        {
            for (ArrayList<String> path : dataPaths)
            {
                this.convertItemsAt(tagIn, path);
            }
        }
    }

    protected void convertItemsAt(NBTTagCompound tag, ArrayList<String> path)
    {
        if (path.size() >= 1)
        {
            final int count = path.size() - 1;

            for (int i = 0; i < count; ++i)
            {
                String tagName = path.get(i);

                if (tag.hasKey(tagName, Constants.NBT.TAG_COMPOUND) == false)
                {
                    return;
                }

                tag = tag.getCompoundTag(tagName);
            }

            String tagName = path.get(count);

            if (tag.hasKey(tagName, Constants.NBT.TAG_LIST))
            {
                this.convertInventoryContents(tag.getTagList(tagName, Constants.NBT.TAG_COMPOUND));
            }
            else if (tag.hasKey(tagName, Constants.NBT.TAG_COMPOUND))
            {
                NBTTagCompound itemTag = tag.getCompoundTag(tagName);
                itemTag = this.convertItemTag(itemTag);
                tag.setTag(tagName, itemTag);
            }
        }
    }

    protected void convertInventoryContents(NBTTagList tagIn)
    {
        final int count = tagIn.tagCount();

        for (int i = 0; i < count; ++i)
        {
            NBTTagCompound itemTag = tagIn.getCompoundTagAt(i);
            tagIn.set(i, this.convertItemTag(itemTag));
        }
    }

    protected NBTTagCompound convertItemTag(NBTTagCompound tagIn)
    {
        this.temporaryIdentity.setFromItem(tagIn);
        ItemIdentity targetIdentity = this.itemConversions.get(this.temporaryIdentity);
        ItemIdentity sourceIdentity = this.itemConversions.inverse().get(targetIdentity);

        if (targetIdentity != null && sourceIdentity != null)
        {
            tagIn = targetIdentity.convertItem(tagIn, sourceIdentity);
        }

        return tagIn;
    }
}
