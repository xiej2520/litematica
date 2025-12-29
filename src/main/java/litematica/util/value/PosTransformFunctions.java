package litematica.util.value;

public class PosTransformFunctions
{
    private static final PosTransform.MutBlockPosFunction[] BLOCK_POS_FUNC = new PosTransform.MutBlockPosFunction[]
    {
        (x, y, z, pos) -> pos.set( x,  y,  z),
        (x, y, z, pos) -> pos.set( x,  y, -z), // mirror Z
        (x, y, z, pos) -> pos.set( x, -y,  z), // mirror Y
        (x, y, z, pos) -> pos.set( x, -y, -z), // +180 X
        (x, y, z, pos) -> pos.set( x,  z,  y),
        (x, y, z, pos) -> pos.set( x, -z,  y), // -90 X
        (x, y, z, pos) -> pos.set( x,  z, -y), // +90 X
        (x, y, z, pos) -> pos.set( x, -z, -y),
        (x, y, z, pos) -> pos.set(-x,  y,  z), // mirror X
        (x, y, z, pos) -> pos.set(-x,  y, -z), // +180 Y
        (x, y, z, pos) -> pos.set(-x, -y,  z), // +180 Z
        (x, y, z, pos) -> pos.set(-x, -y, -z),
        (x, y, z, pos) -> pos.set(-x,  z,  y),
        (x, y, z, pos) -> pos.set(-x, -z,  y),
        (x, y, z, pos) -> pos.set(-x,  z, -y),
        (x, y, z, pos) -> pos.set(-x, -z, -y),
        (x, y, z, pos) -> pos.set( y,  x,  z),
        (x, y, z, pos) -> pos.set( y,  x, -z),
        (x, y, z, pos) -> pos.set(-y,  x,  z), // -90 Z
        (x, y, z, pos) -> pos.set(-y,  x, -z),
        (x, y, z, pos) -> pos.set( z,  x,  y),
        (x, y, z, pos) -> pos.set(-z,  x,  y),
        (x, y, z, pos) -> pos.set( z,  x, -y),
        (x, y, z, pos) -> pos.set(-z,  x, -y), // +90 Y, +90 X
        (x, y, z, pos) -> pos.set( y, -x,  z), // +90 Z
        (x, y, z, pos) -> pos.set( y, -x, -z),
        (x, y, z, pos) -> pos.set(-y, -x,  z),
        (x, y, z, pos) -> pos.set(-y, -x, -z),
        (x, y, z, pos) -> pos.set( z, -x,  y),
        (x, y, z, pos) -> pos.set(-z, -x,  y), // +90 Y, -90 X
        (x, y, z, pos) -> pos.set( z, -x, -y),
        (x, y, z, pos) -> pos.set(-z, -x, -y),
        (x, y, z, pos) -> pos.set( y,  z,  x), // +90 Y, +90 Z
        (x, y, z, pos) -> pos.set( y, -z,  x),
        (x, y, z, pos) -> pos.set(-y,  z,  x),
        (x, y, z, pos) -> pos.set(-y, -z,  x), // +90 Y, -90 Z
        (x, y, z, pos) -> pos.set( z,  y,  x),
        (x, y, z, pos) -> pos.set(-z,  y,  x), // +90 Y
        (x, y, z, pos) -> pos.set( z, -y,  x), // +90 Y, +180 Z
        (x, y, z, pos) -> pos.set(-z, -y,  x),
        (x, y, z, pos) -> pos.set( y,  z, -x),
        (x, y, z, pos) -> pos.set( y, -z, -x),
        (x, y, z, pos) -> pos.set(-y,  z, -x),
        (x, y, z, pos) -> pos.set(-y, -z, -x),
        (x, y, z, pos) -> pos.set( z,  y, -x), // -90 Y
        (x, y, z, pos) -> pos.set(-z,  y, -x),
        (x, y, z, pos) -> pos.set( z, -y, -x),
        (x, y, z, pos) -> pos.set(-z, -y, -x), // +90 Y, +180 X
    };

    private static final PosTransform.MutVec3dFunction[] VEC3D_FUNC = new PosTransform.MutVec3dFunction[]
    {
        (x, y, z, pos) -> pos.set( x,  y,  z),
        (x, y, z, pos) -> pos.set( x,  y, -z), // mirror Z
        (x, y, z, pos) -> pos.set( x, -y,  z), // mirror Y
        (x, y, z, pos) -> pos.set( x, -y, -z), // +180 X
        (x, y, z, pos) -> pos.set( x,  z,  y),
        (x, y, z, pos) -> pos.set( x, -z,  y), // -90 X
        (x, y, z, pos) -> pos.set( x,  z, -y), // +90 X
        (x, y, z, pos) -> pos.set( x, -z, -y),
        (x, y, z, pos) -> pos.set(-x,  y,  z), // mirror X
        (x, y, z, pos) -> pos.set(-x,  y, -z), // +180 Y
        (x, y, z, pos) -> pos.set(-x, -y,  z), // +180 Z
        (x, y, z, pos) -> pos.set(-x, -y, -z),
        (x, y, z, pos) -> pos.set(-x,  z,  y),
        (x, y, z, pos) -> pos.set(-x, -z,  y),
        (x, y, z, pos) -> pos.set(-x,  z, -y),
        (x, y, z, pos) -> pos.set(-x, -z, -y),
        (x, y, z, pos) -> pos.set( y,  x,  z),
        (x, y, z, pos) -> pos.set( y,  x, -z),
        (x, y, z, pos) -> pos.set(-y,  x,  z), // -90 Z
        (x, y, z, pos) -> pos.set(-y,  x, -z),
        (x, y, z, pos) -> pos.set( z,  x,  y),
        (x, y, z, pos) -> pos.set(-z,  x,  y),
        (x, y, z, pos) -> pos.set( z,  x, -y),
        (x, y, z, pos) -> pos.set(-z,  x, -y), // +90 Y, +90 X
        (x, y, z, pos) -> pos.set( y, -x,  z), // +90 Z
        (x, y, z, pos) -> pos.set( y, -x, -z),
        (x, y, z, pos) -> pos.set(-y, -x,  z),
        (x, y, z, pos) -> pos.set(-y, -x, -z),
        (x, y, z, pos) -> pos.set( z, -x,  y),
        (x, y, z, pos) -> pos.set(-z, -x,  y), // +90 Y, -90 X
        (x, y, z, pos) -> pos.set( z, -x, -y),
        (x, y, z, pos) -> pos.set(-z, -x, -y),
        (x, y, z, pos) -> pos.set( y,  z,  x), // +90 Y, +90 Z
        (x, y, z, pos) -> pos.set( y, -z,  x),
        (x, y, z, pos) -> pos.set(-y,  z,  x),
        (x, y, z, pos) -> pos.set(-y, -z,  x), // +90 Y, -90 Z
        (x, y, z, pos) -> pos.set( z,  y,  x),
        (x, y, z, pos) -> pos.set(-z,  y,  x), // +90 Y
        (x, y, z, pos) -> pos.set( z, -y,  x), // +90 Y, +180 Z
        (x, y, z, pos) -> pos.set(-z, -y,  x),
        (x, y, z, pos) -> pos.set( y,  z, -x),
        (x, y, z, pos) -> pos.set( y, -z, -x),
        (x, y, z, pos) -> pos.set(-y,  z, -x),
        (x, y, z, pos) -> pos.set(-y, -z, -x),
        (x, y, z, pos) -> pos.set( z,  y, -x), // -90 Y
        (x, y, z, pos) -> pos.set(-z,  y, -x),
        (x, y, z, pos) -> pos.set( z, -y, -x),
        (x, y, z, pos) -> pos.set(-z, -y, -x), // +90 Y, +180 X
    };

    public static PosTransform.MutBlockPosFunction getBlockPosFunc(int index)
    {
        return BLOCK_POS_FUNC[index];
    }

    public static PosTransform.MutVec3dFunction getVec3dFunc(int index)
    {
        return VEC3D_FUNC[index];
    }
}
