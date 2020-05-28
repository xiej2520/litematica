package fi.dy.masa.litematica.schematic.conversion;

import java.util.HashMap;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NBTTagList;

public class BlockTickDataConverter extends DataConverterBase
{
    private final HashMap<String, String> nameMapping = new HashMap<>();

    public BlockTickDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        super(versionFrom, versionTo, "block_state_map.json", "block_states");
    }

    @Override
    protected void addMapping(JsonObject objFrom, JsonObject objTo)
    {
        // TODO
    }

    public String convertBlockName(String oldName)
    {
        return oldName; // TODO
    }

    public NBTTagList convertBlockNames(NBTTagList blockTickListIn, String idTagName)
    {
        return blockTickListIn; // TODO
    }
}
