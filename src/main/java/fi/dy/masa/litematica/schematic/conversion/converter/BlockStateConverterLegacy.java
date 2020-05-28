package fi.dy.masa.litematica.schematic.conversion.converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class BlockStateConverterLegacy extends DataConverterBase
{
    private final Int2ObjectOpenHashMap<NBTTagCompound> idMetaToState = new Int2ObjectOpenHashMap<>();
    private final Object2IntOpenHashMap<NBTTagCompound> stateToIdMeta = new Object2IntOpenHashMap<>();
    private final boolean isValid;

    public BlockStateConverterLegacy(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        super(versionFrom, versionTo, "block_state_map.json", "block_states");
        this.isValid = versionFrom == MinecraftVersion.MC_1_12_X || versionTo == MinecraftVersion.MC_1_12_X;
    }

    @Override
    protected void addMapping(JsonObject objFrom, JsonObject objTo)
    {

    }

    @Override
    public boolean read(JsonArray arr)
    {
        return true;
    }
}
