package fi.dy.masa.litematica.schematic.container;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class LitematicaPaletteLinear<T> implements ILitematicaPalette<T>
{
    private final T[] values;
    private final IPaletteResizeHandler<T> paletteResizeHandler;
    private final int bits;
    private final int currentCapacity;
    private int currentSize;

    @SuppressWarnings("unchecked")
    public LitematicaPaletteLinear(int bitsIn, IPaletteResizeHandler<T> paletteResizeHandler)
    {
        this.currentCapacity = 1 << bitsIn;
        this.values = (T[]) new Object[this.currentCapacity];
        this.bits = bitsIn;
        this.paletteResizeHandler = paletteResizeHandler;
    }

    @Override
    public int getPaletteSize()
    {
        return this.currentSize;
    }

    @Override
    public int idFor(T value)
    {
        for (int i = 0; i < this.currentSize; ++i)
        {
            if (this.values[i] == value)
            {
                return i;
            }
        }

        final int size = this.currentSize;

        if (size < this.currentCapacity)
        {
            this.values[size] = value;
            ++this.currentSize;
            return size;
        }
        else
        {
            return this.paletteResizeHandler.onResize(this.bits + 1, value, this);
        }
    }

    @Override
    @Nullable
    public T getValue(int id)
    {
        return id >= 0 && id < this.currentSize ? this.values[id] : null;
    }

    @Override
    public List<T> getMapping()
    {
        List<T> list = new ArrayList<>(this.currentSize);

        for (int id = 0; id < this.currentSize; ++id)
        {
            list.add(this.values[id]);
        }

        return list;
    }

    @Override
    public boolean setMapping(List<T> list)
    {
        final int size = list.size();

        if (size <= this.currentCapacity)
        {
            for (int id = 0; id < size; ++id)
            {
                this.values[id] = list.get(id);
            }

            this.currentSize = size;

            return true;
        }

        return false;
    }

    @Override
    public boolean overrideMapping(int id, T value)
    {
        if (id >= 0 && id < this.currentCapacity)
        {
            this.values[id] = value;
            return true;
        }

        return false;
    }

    @Override
    public LitematicaPaletteLinear<T> copy(IPaletteResizeHandler<T> resizeHandler)
    {
        LitematicaPaletteLinear<T> copy = new LitematicaPaletteLinear<>(this.bits, resizeHandler);

        System.arraycopy(this.values, 0, copy.values, 0, this.values.length);
        copy.currentSize = this.currentSize;

        return copy;
    }
}
