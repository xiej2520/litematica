package fi.dy.masa.litematica.schematic.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.litematica.schematic.ISchematicRegion;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public interface ILitematicaPalette<T>
{
    /**
     * Returns the current number of entries in the palette
     * @return
     */
    int getPaletteSize();

    /**
     * Gets the palette id for the given value and adds
     * the value to the palette if it doesn't exist there yet.
     */
    int idFor(T value);

    /**
     * Gets the value by the palette ID, if the provided ID exists.
     */
    @Nullable
    T getValue(int id);

    /**
     * Returns the current full mappings of IDs to values.
     * The ID is the position in the returned list.
     * @return
     */
    List<T> getMapping();

    /**
     * Sets the current mapping of the palette.
     * This is meant for reading the palette from file.
     * @param list
     * @return true if the mapping was set successfully, false if it failed
     */
    boolean setMapping(List<T> list);

    /**
     * Overrides the mapping for the given ID.
     * @param id
     * @param state
     * @return true if the ID was found in the palette and thus possible to override
     */
    boolean overrideMapping(int id, T state);

    /**
     * Creates a copy of this palette, using the provided resize handler
     * @param resizeHandler
     * @return
     */
    ILitematicaPalette<T> copy(IPaletteResizeHandler<T> resizeHandler);

    static <T> int getCombinedPaletteSize(List<ILitematicaPalette<T>> palettes)
    {
        ObjectOpenHashSet<T> set = new ObjectOpenHashSet<>();

        for (ILitematicaPalette<T> palette : palettes)
        {
            set.addAll(palette.getMapping());
        }

        return set.size();
    }

    static ILitematicaPalette<NBTTagCompound> createCombinedPalette(Collection<ISchematicRegion> regions)
    {
        final ArrayList<ILitematicaPalette<NBTTagCompound>> palettes = new ArrayList<>();

        regions.forEach((reg) -> {
            ILitematicaPalette<NBTTagCompound> p = reg.getBlockStateContainer().getTagPalette();
            if (p != null)
            {
                palettes.add(p);
            }
        });

        // Get the total number of unique values in all the palettes
        int capacity = getCombinedPaletteSize(palettes);

        // Create a palette that contains all the values from the containers in the other schematic
        final ILitematicaPalette<NBTTagCompound> combinedTagPalette = LitematicaBlockStateContainerBase.createPaletteWithSize(capacity);

        regions.forEach((reg) -> {
            ILitematicaPalette<NBTTagCompound> p = reg.getBlockStateContainer().getTagPalette();
            if (p != null)
            {
                for (NBTTagCompound tag : p.getMapping())
                {
                    combinedTagPalette.idFor(tag);
                }
            }
        });

        return combinedTagPalette;
    }
}
