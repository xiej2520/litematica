package fi.dy.masa.litematica.schematic.container;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.Litematica;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public abstract class LitematicaBlockStateContainerBase implements ILitematicaBlockStateContainer
{
    public static final IBlockState AIR_BLOCK_STATE = Blocks.AIR.getDefaultState();
    protected static final int MAX_BITS_LINEAR = 4;

    protected ILitematicaPalette<IBlockState> palette;
    protected ILitematicaPalette<NBTTagCompound> tagPalette;

    protected final Vec3i size;
    protected final int sizeX;
    protected final int sizeY;
    protected final int sizeZ;
    protected final long sizeLayer;
    protected final long totalVolume;
    protected long[] blockCounts = new long[0];
    protected int bits;
    protected boolean hasSetBlockCounts;

    public LitematicaBlockStateContainerBase(Vec3i size)
    {
        this(size, 2);
    }

    protected LitematicaBlockStateContainerBase(Vec3i size, int bits)
    {
        this.size = size;
        this.sizeX = size.getX();
        this.sizeY = size.getY();
        this.sizeZ = size.getZ();
        this.sizeLayer = (long) this.sizeX * (long) this.sizeZ;
        this.totalVolume = this.sizeLayer * (long) this.sizeY;

        this.setBits(bits);
    }

    @Override
    public Vec3i getSize()
    {
        return this.size;
    }

    @Override
    public ILitematicaPalette<IBlockState> getPalette()
    {
        return this.palette;
    }

    @Override
    public ILitematicaPalette<NBTTagCompound> getTagPalette()
    {
        return this.tagPalette;
    }

    @Override
    public void setTagPalette(ILitematicaPalette<NBTTagCompound> tagPalette)
    {
        this.tagPalette = tagPalette;
    }

    @Override
    public long getTotalBlockCount()
    {
        this.calculateBlockCountsIfNeeded();

        ILitematicaPalette<IBlockState> palette = this.getPalette();
        IBlockState air = Blocks.AIR.getDefaultState();
        final int length = this.blockCounts.length;
        long count = 0;

        for (int id = 0; id < length; ++id)
        {
            IBlockState state = palette.getValue(id);

            if (state != null && state != air)
            {
                count += this.blockCounts[id];
            }
        }

        return count;
    }

    @Override
    public Map<IBlockState, Long> getBlockCountsMap()
    {
        this.calculateBlockCountsIfNeeded();

        Object2LongOpenHashMap<IBlockState> map = new Object2LongOpenHashMap<>(this.blockCounts.length);
        ILitematicaPalette<IBlockState> palette = this.getPalette();
        final int length = Math.min(palette.getPaletteSize(), this.blockCounts.length);

        for (int id = 0; id < length; ++id)
        {
            IBlockState state = palette.getValue(id);

            if (state != null)
            {
                map.put(state, this.blockCounts[id]);
            }
        }

        return map;
    }

    protected void setBlockCounts(long[] blockCounts)
    {
        final int length = blockCounts.length;

        if (this.blockCounts == null || this.blockCounts.length < length)
        {
            this.blockCounts = new long[length];
        }

        System.arraycopy(blockCounts, 0, this.blockCounts, 0, length);
        this.hasSetBlockCounts = true;
    }

    protected void setBits(int bitsIn)
    {
        this.bits = bitsIn;
    }

    @Override
    public void copyContentsTo(ILitematicaBlockStateContainer other, Vec3i offset, boolean readUsingTagPalette)
    {
        final int offX = offset.getX();
        final int offY = offset.getY();
        final int offZ = offset.getZ();
        Vec3i sizeOther = other.getSize();
        final int sizeX = Math.min(this.size.getX(), sizeOther.getX() - offX);
        final int sizeY = Math.min(this.size.getY(), sizeOther.getY() - offY);
        final int sizeZ = Math.min(this.size.getZ(), sizeOther.getZ() - offZ);

        if (readUsingTagPalette)
        {
            int[] idMapping = this.createRawIdMappingBetween(this.getTagPalette(), other.getTagPalette());

            if (idMapping == null)
            {
                return;
            }

            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        int idThis = this.getRawId(x, y, z);
                        int idOther = idMapping[idThis];

                        if (idOther == -1)
                        {
                            Litematica.logger.error("copyContentsTo(): Failed bit array ID mapping, got ID -1 at position {}, {}, {}", x, y, z);
                            return;
                        }

                        other.setRawId(x + offX, y + offY, z + offZ, idOther);
                    }
                }
            }
        }
        else
        {
            for (int y = 0; y < sizeY; ++y)
            {
                for (int z = 0; z < sizeZ; ++z)
                {
                    for (int x = 0; x < sizeX; ++x)
                    {
                        IBlockState state = this.getBlockState(x, y, z);
                        other.setBlockState(x + offX, y + offY, z + offZ, state);
                    }
                }
            }
        }
    }

    @Nullable
    protected int[] createRawIdMappingBetween(@Nullable ILitematicaPalette<NBTTagCompound> paletteThis,
                                              @Nullable ILitematicaPalette<NBTTagCompound> paletteOther)
    {
        if (paletteThis == null || paletteOther == null || paletteOther.getPaletteSize() < paletteThis.getPaletteSize())
        {
            Litematica.logger.error("createRawIdMappingBetween(): Failed bit array ID mapping, null palette or incorrect size");
            return null;
        }

        Object2IntOpenHashMap<NBTTagCompound> tag2Int = new Object2IntOpenHashMap<>();
        List<NBTTagCompound> mappingListOther = paletteOther.getMapping();
        int size = mappingListOther.size();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = mappingListOther.get(i);
            tag2Int.put(tag, i);
        }

        int[] idMapping = new int[paletteThis.getPaletteSize()];
        Arrays.fill(idMapping, -1);

        List<NBTTagCompound> mappingListThis = paletteThis.getMapping();
        size = mappingListThis.size();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = mappingListThis.get(i);
            int idOther = tag2Int.getOrDefault(tag, -1);

            if (idOther == -1)
            {
                Litematica.logger.error("createRawIdMappingBetween(): Failed bit array ID mapping, got ID -1 for state tag '{}'", tag);
                return null;
            }

            idMapping[i] = idOther;
        }

        return idMapping;
    }

    protected abstract void calculateBlockCountsIfNeeded();

    public static int getBitsForCapacity(int capacity)
    {
        int bits = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(capacity - 1));
        //int bits = Math.max(2, HashCommon.nextPowerOfTwo(capacity + 1));
        return bits;
    }

    public static <T> ILitematicaPalette<T> createPaletteWithSize(int capacity)
    {
        int bits = getBitsForCapacity(capacity);

        if (bits <= MAX_BITS_LINEAR)
        {
            return new LitematicaPaletteLinear<>(bits, null);
        }
        else
        {
            return new LitematicaPaletteHashMap<>(bits, null);
        }
    }

    public static <T> ILitematicaPalette<T> createPalette(int bits, IPaletteResizeHandler<T> resizeHandler)
    {
        if (bits <= MAX_BITS_LINEAR)
        {
            return new LitematicaPaletteLinear<>(bits, resizeHandler);
        }
        else
        {
            return new LitematicaPaletteHashMap<>(bits, resizeHandler);
        }
    }
}
