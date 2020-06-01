package fi.dy.masa.litematica.schematic.container;

import net.minecraft.block.state.IBlockState;

public interface IPaletteResizeHandler<T>
{
    /**
     * Called when a palette runs out of IDs in the current entry width,
     * and the underlying container needs to be resized for the new entry bit width.
     * @param newSizeBits
     * @param valueBeingAdded
     * @param oldPalette
     * @return the ID for the new value being added when the resize happens
     */
    int onResize(int newSizeBits, T valueBeingAdded, ILitematicaPalette<T> oldPalette);
}
