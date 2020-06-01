package fi.dy.masa.litematica.schematic.container;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3i;

public interface ILitematicaBlockStateContainer
{
    Vec3i getSize();

    long getTotalBlockCount();

    Map<IBlockState, Long> getBlockCountsMap();

    ILitematicaPalette<IBlockState> getPalette();

    @Nullable
    ILitematicaPalette<NBTTagCompound> getTagPalette();

    void setTagPalette(ILitematicaPalette<NBTTagCompound> palette);

    IBlockState getBlockState(int x, int y, int z);

    void setBlockState(int x, int y, int z, IBlockState state);

    int getRawId(int x, int y, int z);

    void setRawId(int x, int y, int z, int id);

    void copyContentsTo(ILitematicaBlockStateContainer other, Vec3i offset, boolean readUsingTagPalette);

    ILitematicaBlockStateContainer copy();
}
