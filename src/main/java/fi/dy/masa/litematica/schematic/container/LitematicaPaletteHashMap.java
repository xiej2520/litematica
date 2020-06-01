package fi.dy.masa.litematica.schematic.container;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.IntIdentityHashBiMap;

public class LitematicaPaletteHashMap<T> implements ILitematicaPalette<T>
{
    protected final IntIdentityHashBiMap<T> statePaletteMap;
    protected final IPaletteResizeHandler<T> paletteResizeHandler;
    protected final int bits;

    public LitematicaPaletteHashMap(int bitsIn, IPaletteResizeHandler<T> paletteResizeHandler)
    {
        this.bits = bitsIn;
        this.paletteResizeHandler = paletteResizeHandler;
        this.statePaletteMap = new IntIdentityHashBiMap<>(1 << bitsIn);
    }

    @Override
    public int idFor(T value)
    {
        int id = this.statePaletteMap.getId(value);

        if (id == -1)
        {
            id = this.statePaletteMap.add(value);

            if (id >= (1 << this.bits))
            {
                id = this.paletteResizeHandler.onResize(this.bits + 1, value, this);
            }
        }

        return id;
    }

    @Override
    @Nullable
    public T getValue(int indexKey)
    {
        return this.statePaletteMap.get(indexKey);
    }

    @Override
    public int getPaletteSize()
    {
        return this.statePaletteMap.size();
    }

    @Override
    public List<T> getMapping()
    {
        final int size = this.statePaletteMap.size();
        List<T> list = new ArrayList<>(size);

        for (int id = 0; id < size; ++id)
        {
            list.add(this.statePaletteMap.get(id));
        }

        return list;
    }

    @Override
    public boolean setMapping(List<T> list)
    {
        this.statePaletteMap.clear();

        for (T value : list)
        {
            this.statePaletteMap.add(value);
        }

        return true;
    }

    @Override
    public boolean overrideMapping(int id, T state)
    {
        List<T> mapping = this.getMapping();

        if (id >= 0 && id < mapping.size())
        {
            // The put method of the map doesn't work for this, it increases the size etc. :/
            mapping.set(id, state);
            this.setMapping(mapping);
            return true;
        }

        return false;
    }

    @Override
    public LitematicaPaletteHashMap<T> copy(IPaletteResizeHandler<T> resizeHandler)
    {
        LitematicaPaletteHashMap<T> copy = new LitematicaPaletteHashMap<>(this.bits, resizeHandler);

        for (int id = 0; id < this.statePaletteMap.size(); ++id)
        {
            copy.statePaletteMap.add(this.statePaletteMap.get(id));
        }

        return copy;
    }
}
