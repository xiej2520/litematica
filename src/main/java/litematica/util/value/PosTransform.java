package litematica.util.value;

import com.google.common.collect.ImmutableList;

import malilib.util.position.BlockPos;

public enum PosTransform
{
    //XPX_YPY_ZPZ((x, y, z, pos) -> pos.set( x,  y,  z), (x, y, z, pos) -> pos.set( x,  y,  z)),  // no transform
    NONE       ((x, y, z, pos) -> pos.set( x,  y,  z), (x, y, z, pos) -> pos.set( x,  y,  z)),  // no transform

    XNX_YPY_ZPZ((x, y, z, pos) -> pos.set(-x,  y,  z), (x, y, z, pos) -> pos.set(-x,  y,  z)),  // mirror X
    XPX_YNY_ZPZ((x, y, z, pos) -> pos.set( x, -y,  z), (x, y, z, pos) -> pos.set( x, -y,  z)),  // mirror Y
    XPX_YPY_ZNZ((x, y, z, pos) -> pos.set( x,  y, -z), (x, y, z, pos) -> pos.set( x,  y, -z)),  // mirror Z

    XPZ_YPY_ZNX((x, y, z, pos) -> pos.set(-z,  y,  x), (x, y, z, pos) -> pos.set( z,  y, -x)),  // +90 Y
    XNX_YPY_ZNZ((x, y, z, pos) -> pos.set(-x,  y, -z), (x, y, z, pos) -> pos.set(-x,  y, -z)),  // +180 Y
    XNZ_YPY_ZPX((x, y, z, pos) -> pos.set( z,  y, -x), (x, y, z, pos) -> pos.set(-z,  y,  x)),  // -90 Y

    XPX_YNZ_ZPY((x, y, z, pos) -> pos.set( x,  z, -y), (x, y, z, pos) -> pos.set( x, -z,  y)),  // +90 X
    XPX_YNY_ZNZ((x, y, z, pos) -> pos.set( x, -y, -z), (x, y, z, pos) -> pos.set( x, -y, -z)),  // +180 X
    XPX_YPZ_ZNY((x, y, z, pos) -> pos.set( x, -z,  y), (x, y, z, pos) -> pos.set( x,  z, -y)),  // -90 X

    XNY_YPX_ZPZ((x, y, z, pos) -> pos.set( y, -x,  z), (x, y, z, pos) -> pos.set(-y,  x,  z)),  // +90 Z
    XNX_YNY_ZPZ((x, y, z, pos) -> pos.set(-x, -y,  z), (x, y, z, pos) -> pos.set(-x, -y,  z)),  // +180 Z
    XPY_YNX_ZPZ((x, y, z, pos) -> pos.set(-y,  x,  z), (x, y, z, pos) -> pos.set( y, -x,  z)),  // -90 Z

    XPY_YNZ_ZNX((x, y, z, pos) -> pos.set(-z,  x, -y), (x, y, z, pos) -> pos.set( y,  z, -x)),  // +90 Y, +90 X
    XNZ_YNY_ZNX((x, y, z, pos) -> pos.set(-z, -y, -x), (x, y, z, pos) -> pos.set(-z, -y, -x)),  // +90 Y, +180 X
    XNY_YPZ_ZNX((x, y, z, pos) -> pos.set(-z, -x,  y), (x, y, z, pos) -> pos.set( y, -z, -x))  // +90 Y, -90 X
    ;

    public static final ImmutableList<PosTransform> VALUES = ImmutableList.copyOf(values());

    private final PositionFunction forwardFunc;
    private final PositionFunction reverseFunc;

    PosTransform(PositionFunction forwardFunc, PositionFunction reverseFunc)
    {
        this.forwardFunc = forwardFunc;
        this.reverseFunc = reverseFunc;
    }

    public BlockPos.MutBlockPos apply(BlockPos.MutBlockPos mutablePos)
    {
        return this.apply(mutablePos.getX(), mutablePos.getY(), mutablePos.getZ(), mutablePos);
    }

    public BlockPos.MutBlockPos apply(int x, int y, int z, BlockPos.MutBlockPos mutablePos)
    {
        this.forwardFunc.apply(x, y, z, mutablePos);
        return mutablePos;
    }

    public BlockPos.MutBlockPos reverse(BlockPos.MutBlockPos mutablePos)
    {
        return this.reverse(mutablePos.getX(), mutablePos.getY(), mutablePos.getZ(), mutablePos);
    }

    public BlockPos.MutBlockPos reverse(int x, int y, int z, BlockPos.MutBlockPos mutablePos)
    {
        this.reverseFunc.apply(x, y, z, mutablePos);
        return mutablePos;
    }

    public interface PositionFunction
    {
        void apply(int x, int y, int z, BlockPos.MutBlockPos mutablePos);
    }
}
