package litematica.util.value;

import com.google.common.collect.ImmutableList;

import malilib.util.position.BlockPos;
import malilib.util.position.Vec3d;

public enum PosTransform
{
    //XPX_YPY_ZPZ((x, y, z, pos) -> pos.set( x,  y,  z), 0),
    NONE       ( 0,  0), // no-op / no transform
    XPX_YPY_ZNZ( 1,  1), // mirror Z
    XPX_YNY_ZPZ( 2,  2), // mirror Y
    XPX_YNY_ZNZ( 3,  3), // +180 X
    XPX_YPZ_ZPY( 4,  4), // -90 X, mirror Y
    XPX_YPZ_ZNY( 5,  6), // -90 X
    XPX_YNZ_ZPY( 6,  5), // +90 X
    XPX_YNZ_ZNY( 7,  7), // +90 X, mirror Y
    XNX_YPY_ZPZ( 8,  8), // mirror X
    XNX_YPY_ZNZ( 9,  9), // +180 Y
    XNX_YNY_ZPZ(10, 10), // +180 Z
    XNX_YNY_ZNZ(11, 11), // +180 Z, mirror Z
    XNX_YPZ_ZPY(12, 12), // +180 Z, +90 X
    XNX_YPZ_ZNY(13, 14), // -90 X, mirror X
    XNX_YNZ_ZPY(14, 13), // +90 X, mirror X
    XNX_YNZ_ZNY(15, 15), // +90 X, +180 Z
    XPY_YPX_ZPZ(16, 16), // -90 Z, mirror X
    XPY_YPX_ZNZ(17, 17), // -90 Z, +180 Y
    XPY_YNX_ZPZ(18, 24), // -90 Z
    XPY_YNX_ZNZ(19, 25), // -90 Z, mirror Z
    XPY_YPZ_ZPX(20, 32), // -90 Z, -90 Y
    XPY_YPZ_ZNX(21, 40), // -90 Z, -90 Y, mirror X
    XPY_YNZ_ZPX(22, 33), // -90 Z, +90 Y, mirror X
    XPY_YNZ_ZNX(23, 41), // +90 Y, +90 X
    XNY_YPX_ZPZ(24, 18), // +90 Z
    XNY_YPX_ZNZ(25, 19), // +90 Z, mirror Z
    XNY_YNX_ZPZ(26, 26), // +90 Z, mirror X
    XNY_YNX_ZNZ(27, 27), // +90 Z, +180 Y
    XNY_YPZ_ZPX(28, 34), // +90 Z, +90 Y, mirror X or -90 Y, -90 X, mirror Y
    XNY_YPZ_ZNX(29, 42), // +90 Y, -90 X
    XNY_YNZ_ZPX(30, 35), // +90 Z, -90 Y
    XNY_YNZ_ZNX(31, 43), // +90 Z, -90 Y, mirror X or +90 X, +90 Z, mirror X
    XPZ_YPX_ZPY(32, 20), // +90 Y, +90 Z
    XPZ_YPX_ZNY(33, 22), // +90 Y, +90 Z, mirror Y or +90 Z, +90 X, mirror Y
    XPZ_YNX_ZPY(34, 28), // +90 Y, -90 Z, mirror Y or -90 Z, -90 X, mirror Y or -90 Z, +90 X, mirror Z
    XPZ_YNX_ZNY(35, 30), // +90 Y, -90 Z
    XPZ_YPY_ZPX(36, 36), // +90 Y, mirror X or -90 Y, mirror Z
    XPZ_YPY_ZNX(37, 44), // +90 Y
    XPZ_YNY_ZPX(38, 38), // +90 Y, +180 Z
    XPZ_YNY_ZNX(39, 46), // +90 Y, mirror Y
    XNZ_YPX_ZPY(40, 21), // -90 Y, +90 Z, mirror Y
    XNZ_YPX_ZNY(41, 23), // -90 Y, +90 Z
    XNZ_YNX_ZPY(42, 29), // -90 Y, -90 Z
    XNZ_YNX_ZNY(43, 31), // -90 Y, -90 Z, mirror Y
    XNZ_YPY_ZPX(44, 37), // -90 Y
    XNZ_YPY_ZNX(45, 45), // -90 Y, mirror X or +90 Y, mirror Z
    XNZ_YNY_ZPX(46, 39), // -90 Y, mirror Y
    XNZ_YNY_ZNX(47, 47); // +90 Y, +180 X

    private static final PosTransform[] VALUES_ARR = values();
    public static final ImmutableList<PosTransform> VALUES = ImmutableList.copyOf(VALUES_ARR);

    private final MutBlockPosFunction mutBlockPosFunction;
    private final MutVec3dFunction mutVec3dFunction;
    private final int index;
    private final int indexOfReverse;

    PosTransform(int index, int indexOfReverse)
    {
        this.index = index;
        this.indexOfReverse = indexOfReverse;
        this.mutBlockPosFunction = PosTransformFunctions.getBlockPosFunc(index);
        this.mutVec3dFunction = PosTransformFunctions.getVec3dFunc(index);
    }

    public BlockPos.MutBlockPos apply(BlockPos.MutBlockPos mutPos)
    {
        return this.apply(mutPos.getX(), mutPos.getY(), mutPos.getZ(), mutPos);
    }

    public BlockPos.MutBlockPos apply(int x, int y, int z, BlockPos.MutBlockPos mutPos)
    {
        this.mutBlockPosFunction.apply(x, y, z, mutPos);
        return mutPos;
    }

    public Vec3d.MutVec3d apply(Vec3d.MutVec3d mutVec)
    {
        return this.apply(mutVec.getX(), mutVec.getY(), mutVec.getZ(), mutVec);
    }

    public Vec3d.MutVec3d apply(double x, double y, double z, Vec3d.MutVec3d mutVec)
    {
        this.mutVec3dFunction.apply(x, y, z, mutVec);
        return mutVec;
    }

    public PosTransform getReverse()
    {
        return VALUES_ARR[this.indexOfReverse];
    }

    public BlockPos.MutBlockPos reverse(BlockPos.MutBlockPos mutPos)
    {
        return this.reverse(mutPos.getX(), mutPos.getY(), mutPos.getZ(), mutPos);
    }

    public BlockPos.MutBlockPos reverse(int x, int y, int z, BlockPos.MutBlockPos mutPos)
    {
        this.getReverse().apply(x, y, z, mutPos);
        return mutPos;
    }

    public interface MutBlockPosFunction
    {
        void apply(int x, int y, int z, BlockPos.MutBlockPos mutPos);
    }

    public interface MutVec3dFunction
    {
        void apply(double x, double y, double z, Vec3d.MutVec3d mutVec);
    }
}
