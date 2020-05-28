package fi.dy.masa.litematica.schematic.conversion;

import net.minecraft.nbt.NBTTagList;

public interface IListTagDataConverter
{
    /**
     * Converts the data in the given list (in-place!).
     * @param tagList The list tag holding the entries to convert
     * @param idTagName The tag name containing the ID of each entry, which is used
     *                  to fetch the correct mapping entry or converter for each object type.
     */
    void convertData(NBTTagList tagList, String idTagName);
}
