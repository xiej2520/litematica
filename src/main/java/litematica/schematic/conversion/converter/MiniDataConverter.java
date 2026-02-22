package litematica.schematic.conversion.converter;

import malilib.util.data.Constants;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.ListData;

public interface MiniDataConverter {
    boolean convertBlockStateData(CompoundData data);
    boolean convertEntityData(CompoundData data);
    boolean convertBlockEntityData(CompoundData blockEntityTag);
    boolean convertItemData(CompoundData itemTag);

    default void convertItemsList(ListData items) {
        for (int i = 0; i < items.size(); i++) {
            CompoundData itemTag = items.getCompoundAt(i);
            if (itemTag != null && itemTag.isEmpty() == false) {
                convertItemData(itemTag);
            }
        }
    }

    default void convertItemsListIfKey(CompoundData data, String key) {
        if (data.containsList(key, Constants.NBT.TAG_COMPOUND)) {
            ListData items = data.getList(key, Constants.NBT.TAG_COMPOUND);
            convertItemsList(items);
        }
    }
}
