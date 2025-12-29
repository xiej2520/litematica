package litematica.util.value;

import com.google.common.collect.ImmutableList;

import malilib.util.position.BlockPos;
import malilib.util.position.Vec3d;

public enum PosTransform
{
    //XPX_YPY_ZPZ((x, y, z, pos) -> pos.set( x,  y,  z), 0),
    NONE       ((x, y, z, pos) -> pos.set( x,  y,  z), (x, y, z, pos) -> pos.set( x,  y,  z),  0),
    XPX_YPY_ZNZ((x, y, z, pos) -> pos.set( x,  y, -z), (x, y, z, pos) -> pos.set( x,  y, -z),  1), // mirror Z
    XPX_YNY_ZPZ((x, y, z, pos) -> pos.set( x, -y,  z), (x, y, z, pos) -> pos.set( x, -y,  z),  2), // mirror Y
    XPX_YNY_ZNZ((x, y, z, pos) -> pos.set( x, -y, -z), (x, y, z, pos) -> pos.set( x, -y, -z),  3), // +180 X
    XPX_YPZ_ZPY((x, y, z, pos) -> pos.set( x,  z,  y), (x, y, z, pos) -> pos.set( x,  z,  y),  4),
    XPX_YPZ_ZNY((x, y, z, pos) -> pos.set( x, -z,  y), (x, y, z, pos) -> pos.set( x, -z,  y),  6), // -90 X
    XPX_YNZ_ZPY((x, y, z, pos) -> pos.set( x,  z, -y), (x, y, z, pos) -> pos.set( x,  z, -y),  5), // +90 X
    XPX_YNZ_ZNY((x, y, z, pos) -> pos.set( x, -z, -y), (x, y, z, pos) -> pos.set( x, -z, -y),  7),
    XNX_YPY_ZPZ((x, y, z, pos) -> pos.set(-x,  y,  z), (x, y, z, pos) -> pos.set(-x,  y,  z),  8), // mirror X
    XNX_YPY_ZNZ((x, y, z, pos) -> pos.set(-x,  y, -z), (x, y, z, pos) -> pos.set(-x,  y, -z),  9), // +180 Y
    XNX_YNY_ZPZ((x, y, z, pos) -> pos.set(-x, -y,  z), (x, y, z, pos) -> pos.set(-x, -y,  z), 10), // +180 Z
    XNX_YNY_ZNZ((x, y, z, pos) -> pos.set(-x, -y, -z), (x, y, z, pos) -> pos.set(-x, -y, -z), 11),
    XNX_YPZ_ZPY((x, y, z, pos) -> pos.set(-x,  z,  y), (x, y, z, pos) -> pos.set(-x,  z,  y), 12),
    XNX_YPZ_ZNY((x, y, z, pos) -> pos.set(-x, -z,  y), (x, y, z, pos) -> pos.set(-x, -z,  y), 14),
    XNX_YNZ_ZPY((x, y, z, pos) -> pos.set(-x,  z, -y), (x, y, z, pos) -> pos.set(-x,  z, -y), 13),
    XNX_YNZ_ZNY((x, y, z, pos) -> pos.set(-x, -z, -y), (x, y, z, pos) -> pos.set(-x, -z, -y), 15),
    XPY_YPX_ZPZ((x, y, z, pos) -> pos.set( y,  x,  z), (x, y, z, pos) -> pos.set( y,  x,  z), 16),
    XPY_YPX_ZNZ((x, y, z, pos) -> pos.set( y,  x, -z), (x, y, z, pos) -> pos.set( y,  x, -z), 17),
    XPY_YNX_ZPZ((x, y, z, pos) -> pos.set(-y,  x,  z), (x, y, z, pos) -> pos.set(-y,  x,  z), 24), // -90 Z
    XPY_YNX_ZNZ((x, y, z, pos) -> pos.set(-y,  x, -z), (x, y, z, pos) -> pos.set(-y,  x, -z), 25),
    XPY_YPZ_ZPX((x, y, z, pos) -> pos.set( z,  x,  y), (x, y, z, pos) -> pos.set( z,  x,  y), 32),
    XPY_YPZ_ZNX((x, y, z, pos) -> pos.set(-z,  x,  y), (x, y, z, pos) -> pos.set(-z,  x,  y), 40),
    XPY_YNZ_ZPX((x, y, z, pos) -> pos.set( z,  x, -y), (x, y, z, pos) -> pos.set( z,  x, -y), 33),
    XPY_YNZ_ZNX((x, y, z, pos) -> pos.set(-z,  x, -y), (x, y, z, pos) -> pos.set(-z,  x, -y), 41), // +90 Y, +90 X
    XNY_YPX_ZPZ((x, y, z, pos) -> pos.set( y, -x,  z), (x, y, z, pos) -> pos.set( y, -x,  z), 18), // +90 Z
    XNY_YPX_ZNZ((x, y, z, pos) -> pos.set( y, -x, -z), (x, y, z, pos) -> pos.set( y, -x, -z), 19),
    XNY_YNX_ZPZ((x, y, z, pos) -> pos.set(-y, -x,  z), (x, y, z, pos) -> pos.set(-y, -x,  z), 26),
    XNY_YNX_ZNZ((x, y, z, pos) -> pos.set(-y, -x, -z), (x, y, z, pos) -> pos.set(-y, -x, -z), 27),
    XNY_YPZ_ZPX((x, y, z, pos) -> pos.set( z, -x,  y), (x, y, z, pos) -> pos.set( z, -x,  y), 34),
    XNY_YPZ_ZNX((x, y, z, pos) -> pos.set(-z, -x,  y), (x, y, z, pos) -> pos.set(-z, -x,  y), 42), // +90 Y, -90 X
    XNY_YNZ_ZPX((x, y, z, pos) -> pos.set( z, -x, -y), (x, y, z, pos) -> pos.set( z, -x, -y), 35),
    XNY_YNZ_ZNX((x, y, z, pos) -> pos.set(-z, -x, -y), (x, y, z, pos) -> pos.set(-z, -x, -y), 43),
    XPZ_YPX_ZPY((x, y, z, pos) -> pos.set( y,  z,  x), (x, y, z, pos) -> pos.set( y,  z,  x), 20), // +90 Y, +90 Z
    XPZ_YPX_ZNY((x, y, z, pos) -> pos.set( y, -z,  x), (x, y, z, pos) -> pos.set( y, -z,  x), 22),
    XPZ_YNX_ZPY((x, y, z, pos) -> pos.set(-y,  z,  x), (x, y, z, pos) -> pos.set(-y,  z,  x), 28),
    XPZ_YNX_ZNY((x, y, z, pos) -> pos.set(-y, -z,  x), (x, y, z, pos) -> pos.set(-y, -z,  x), 30), // +90 Y, -90 Z
    XPZ_YPY_ZPX((x, y, z, pos) -> pos.set( z,  y,  x), (x, y, z, pos) -> pos.set( z,  y,  x), 36),
    XPZ_YPY_ZNX((x, y, z, pos) -> pos.set(-z,  y,  x), (x, y, z, pos) -> pos.set(-z,  y,  x), 44), // +90 Y
    XPZ_YNY_ZPX((x, y, z, pos) -> pos.set( z, -y,  x), (x, y, z, pos) -> pos.set( z, -y,  x), 38), // +90 Y, +180 Z
    XPZ_YNY_ZNX((x, y, z, pos) -> pos.set(-z, -y,  x), (x, y, z, pos) -> pos.set(-z, -y,  x), 46),
    XNZ_YPX_ZPY((x, y, z, pos) -> pos.set( y,  z, -x), (x, y, z, pos) -> pos.set( y,  z, -x), 21),
    XNZ_YPX_ZNY((x, y, z, pos) -> pos.set( y, -z, -x), (x, y, z, pos) -> pos.set( y, -z, -x), 23),
    XNZ_YNX_ZPY((x, y, z, pos) -> pos.set(-y,  z, -x), (x, y, z, pos) -> pos.set(-y,  z, -x), 29),
    XNZ_YNX_ZNY((x, y, z, pos) -> pos.set(-y, -z, -x), (x, y, z, pos) -> pos.set(-y, -z, -x), 31),
    XNZ_YPY_ZPX((x, y, z, pos) -> pos.set( z,  y, -x), (x, y, z, pos) -> pos.set( z,  y, -x), 37), // -90 Y
    XNZ_YPY_ZNX((x, y, z, pos) -> pos.set(-z,  y, -x), (x, y, z, pos) -> pos.set(-z,  y, -x), 45),
    XNZ_YNY_ZPX((x, y, z, pos) -> pos.set( z, -y, -x), (x, y, z, pos) -> pos.set( z, -y, -x), 39),
    XNZ_YNY_ZNX((x, y, z, pos) -> pos.set(-z, -y, -x), (x, y, z, pos) -> pos.set(-z, -y, -x), 47); // +90 Y, +180 X

    private static final PosTransform[] VALUES_ARR = values();
    public static final ImmutableList<PosTransform> VALUES = ImmutableList.copyOf(VALUES_ARR);

    private final MutBlockPosFunction mutBlockPosFunction;
    private final MutVec3dFunction mutVec3dFunction;
    private final int indexOfReverse;

    PosTransform(MutBlockPosFunction mutBlockPosFunction, MutVec3dFunction mutVec3dFunction, int indexOfReverse)
    {
        this.mutBlockPosFunction = mutBlockPosFunction;
        this.mutVec3dFunction = mutVec3dFunction;
        this.indexOfReverse = indexOfReverse;
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
