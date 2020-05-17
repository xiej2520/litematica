package fi.dy.masa.litematica.schematic.conversion;

import com.google.gson.JsonArray;
import net.minecraft.nbt.NBTTagCompound;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BlockStateConverterLegacy implements IBlockStateMapReader
{
    private final Int2ObjectOpenHashMap<NBTTagCompound> idMetaToState = new Int2ObjectOpenHashMap<>();
    private final Object2IntOpenHashMap<NBTTagCompound> stateToIdMeta = new Object2IntOpenHashMap<>();

    @Override
    public boolean read(JsonArray arr)
    {
        return true;
    }
}
