package fi.dy.masa.litematica.schematic;

import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.NextTickListEntry;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;

public interface ISchematicRegion
{
    /**
     * Returns the relative position of this region in relation to the origin of the entire schematic.
     * @return
     */
    BlockPos getPosition();

    /**
     * Returns the size of this region.
     * <b>Note:</b> The size can be negative, if the second corner is on the negative side
     * on any axis compared to the primary/origin corner.
     * @return
     */
    Vec3i getSize();

    /**
     * Returns the block state container used for storing the block states in this region
     * @return
     */
    ILitematicaBlockStateContainer getBlockStateContainer();

    /**
     * Returns the entity list for this region
     * @return
     */
    ImmutableList<EntityInfo> getEntityList();

    /**
     * Returns the BlockEntity map used for this region
     * @return
     */
    ImmutableMap<BlockPos, NBTTagCompound> getBlockEntityMap();

    /*
     * Returns the map for the scheduled Block ticks in this region
     */
    ImmutableMap<BlockPos, NextTickListEntry> getBlockTickMap();

    /**
     * Sets the entity list, replacing the old list
     * @param list
     */
    void setEntityList(List<EntityInfo> list);

    /**
     * Sets the block entity map, replacing the old map
     * @param map
     */
    void setBlockEntityMap(Map<BlockPos, NBTTagCompound> map);

    /**
     * Sets the block tick map, replacing the old map
     * @param map
     */
    void setBlockTickMap(Map<BlockPos, NextTickListEntry> map);
}
