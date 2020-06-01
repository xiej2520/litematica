package fi.dy.masa.litematica.schematic.container;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.Vec3i;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public class LitematicaBlockStateContainerSparse extends LitematicaBlockStateContainerBase
{
    private final Long2IntOpenHashMap blocks = new Long2IntOpenHashMap();

    public LitematicaBlockStateContainerSparse(Vec3i size)
    {
        super(size);

        this.palette = new VanillaStructurePalette();
        this.blockCounts = new long[256];
        this.blocks.defaultReturnValue(-1);
    }

    @Override
    public int getRawId(int x, int y, int z)
    {
        long pos = (long) y << 32 | (long) (z & 0xFFFF) << 16 | (long) (x & 0xFFFF);
        return this.blocks.get(pos);
    }

    @Override
    public IBlockState getBlockState(int x, int y, int z)
    {
        IBlockState state = this.palette.getValue(this.getRawId(x, y, z));
        return state != null ? state : AIR_BLOCK_STATE;
    }

    @Override
    public void setRawId(int x, int y, int z, int id)
    {
        long pos = (long) y << 32 | (long) (z & 0xFFFF) << 16 | (long) (x & 0xFFFF);
        int oldId = this.blocks.get(pos);

        if (oldId != id)
        {
            this.blocks.put(pos, id);

            if (id >= this.blockCounts.length)
            {
                long[] oldArr = this.blockCounts;
                this.blockCounts = new long[oldArr.length * 2];
                System.arraycopy(oldArr, 0, this.blockCounts, 0, oldArr.length);
            }

            if (oldId >= 0)
            {
                --this.blockCounts[oldId];
            }

            ++this.blockCounts[id];
        }
    }

    @Override
    public void setBlockState(int x, int y, int z, IBlockState state)
    {
        long pos = (long) y << 32 | (long) (z & 0xFFFF) << 16 | (long) (x & 0xFFFF);
        int oldId = this.blocks.get(pos);
        IBlockState oldState = this.palette.getValue(oldId);

        if (oldState != state)
        {
            int id = this.palette.idFor(state);
            this.blocks.put(pos, id);

            if (id >= this.blockCounts.length)
            {
                long[] oldArr = this.blockCounts;
                this.blockCounts = new long[oldArr.length * 2];
                System.arraycopy(oldArr, 0, this.blockCounts, 0, oldArr.length);
            }

            if (oldState != null)
            {
                --this.blockCounts[oldId];
            }

            ++this.blockCounts[id];
        }
    }

    @Override
    public LitematicaBlockStateContainerSparse copy()
    {
        LitematicaBlockStateContainerSparse copy = new LitematicaBlockStateContainerSparse(this.size);

        copy.blocks.putAll(this.blocks);
        copy.blockCounts = this.blockCounts.clone();
        copy.palette = this.palette.copy(null);
        copy.tagPalette = this.tagPalette != null ? this.tagPalette.copy(null) : null;

        return copy;
    }

    @Override
    protected void calculateBlockCountsIfNeeded()
    {
    }

    public Long2IntOpenHashMap getBlockMap()
    {
        return this.blocks;
    }
}
