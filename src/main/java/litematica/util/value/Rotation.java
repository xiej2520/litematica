package litematica.util.value;

import java.util.Arrays;

import malilib.util.position.BlockMirror;
import malilib.util.position.BlockPos;
import malilib.util.position.BlockRotation;
import malilib.util.position.Direction;
import malilib.util.position.Vec3d;

public class Rotation
{
    public static final Rotation NONE      = new Rotation(new int[] { 1,  0,  0,    0,  1,  0,    0,  0,  1}, new int[] { 1,  0,  0,    0,  1,  0,    0,  0,  1});

    public static final Rotation ROT_X_90  = new Rotation(new int[] { 1,  0,  0,    0,  0,  1,    0, -1,  0}, new int[] { 1,  0,  0,    0,  0, -1,    0,  1,  0});
    public static final Rotation ROT_X_180 = new Rotation(new int[] { 1,  0,  0,    0, -1,  0,    0,  0, -1}, new int[] { 1,  0,  0,    0, -1,  0,    0,  0, -1});
    public static final Rotation ROT_X_270 = new Rotation(new int[] { 1,  0,  0,    0,  0, -1,    0,  1,  0}, new int[] { 1,  0,  0,    0,  0,  1,    0, -1,  0});

    public static final Rotation ROT_Y_90  = new Rotation(new int[] { 0,  0, -1,    0,  1,  0,    1,  0,  0}, new int[] { 0,  0,  1,    0,  1,  0,   -1,  0,  0});
    public static final Rotation ROT_Y_180 = new Rotation(new int[] {-1,  0,  0,    0,  1,  0,    0,  0, -1}, new int[] {-1,  0,  0,    0,  1,  0,    0,  0, -1});
    public static final Rotation ROT_Y_270 = new Rotation(new int[] { 0,  0,  1,    0,  1,  0,   -1,  0,  0}, new int[] { 0,  0, -1,    0,  1,  0,    1,  0,  0});

    public static final Rotation ROT_Z_90  = new Rotation(new int[] { 0,  1,  0,   -1,  0,  0,    0,  0,  1}, new int[] { 0, -1,  0,    1,  0,  0,    0,  0,  1});
    public static final Rotation ROT_Z_180 = new Rotation(new int[] {-1,  0,  0,    0, -1,  0,    0,  0,  1}, new int[] {-1,  0,  0,    0, -1,  0,    0,  0,  1});
    public static final Rotation ROT_Z_270 = new Rotation(new int[] { 0, -1,  0,    1,  0,  0,    0,  0,  1}, new int[] { 0,  1,  0,   -1,  0,  0,    0,  0,  1});

    public static final Rotation MIRROR_X  = new Rotation(new int[] {-1,  0,  0,    0,  1,  0,    0,  0,  1}, new int[] { 1,  0,  0,    0,  1,  0,    0,  0, -1});
    public static final Rotation MIRROR_Y  = new Rotation(new int[] { 1,  0,  0,    0, -1,  0,    0,  0,  1}, new int[] { 1,  0,  0,    0, -1,  0,    0,  0,  1});
    public static final Rotation MIRROR_Z  = new Rotation(new int[] { 1,  0,  0,    0,  1,  0,    0,  0, -1}, new int[] {-1,  0,  0,    0,  1,  0,    0,  0,  1});

    private final int[] forwardMatrix;
    private final int[] reverseMatrix;

    public Rotation(int[] forwardMatrix, int[] reverseMatrix)
    {
        this.forwardMatrix = forwardMatrix;
        this.reverseMatrix = reverseMatrix;
    }

    public Rotation add(Rotation other)
    {
        int[] f = multiply(other.forwardMatrix, this.forwardMatrix);

        if (Arrays.equals(f, NONE.forwardMatrix))
        {
            return NONE;
        }

        int[] r = multiply(this.reverseMatrix, other.reverseMatrix);

        return new Rotation(f, r);
    }

    public int[] getForwardMatrix()
    {
        return this.forwardMatrix;
    }

    public int[] getReverseMatrix()
    {
        return this.reverseMatrix;
    }

    public BlockPos.MutBlockPos rotate(BlockPos.MutBlockPos pos)
    {
        if (this == NONE)
            return pos;

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int[] m = this.forwardMatrix;

        int newX = x * m[0] + y * m[1] + z * m[2];
        int newY = x * m[3] + y * m[4] + z * m[5];
        int newZ = x * m[6] + y * m[7] + z * m[8];

        pos.set(newX, newY, newZ);

        return pos;
    }

    public BlockPos.MutBlockPos reverse(BlockPos.MutBlockPos pos)
    {
        if (this == NONE)
            return pos;

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int[] m = this.reverseMatrix;

        int newX = x * m[0] + y * m[1] + z * m[2];
        int newY = x * m[3] + y * m[4] + z * m[5];
        int newZ = x * m[6] + y * m[7] + z * m[8];

        pos.set(newX, newY, newZ);

        return pos;
    }

    public Vec3d.MutVec3d rotate(Vec3d.MutVec3d pos)
    {
        if (this == NONE)
            return pos;

        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        int[] m = this.forwardMatrix;

        double newX = x * m[0] + y * m[1] + z * m[2];
        double newY = x * m[3] + y * m[4] + z * m[5];
        double newZ = x * m[6] + y * m[7] + z * m[8];

        pos.set(newX, newY, newZ);

        return pos;
    }

    public Vec3d.MutVec3d reverse(Vec3d.MutVec3d pos)
    {
        if (this == NONE)
            return pos;

        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        int[] m = this.reverseMatrix;

        double newX = x * m[0] + y * m[1] + z * m[2];
        double newY = x * m[3] + y * m[4] + z * m[5];
        double newZ = x * m[6] + y * m[7] + z * m[8];

        pos.set(newX, newY, newZ);

        return pos;
    }

    public static int[] multiply(int[] m1, int[] m2)
    {
        int[] t = m1;
        int[] o = m2;

        int n11 = t[0] * o[0] + t[1] * o[3] + t[2] * o[6];
        int n21 = t[3] * o[0] + t[4] * o[3] + t[5] * o[6];
        int n31 = t[6] * o[0] + t[7] * o[3] + t[8] * o[6];

        int n12 = t[0] * o[1] + t[1] * o[4] + t[2] * o[7];
        int n22 = t[3] * o[1] + t[4] * o[4] + t[5] * o[7];
        int n32 = t[6] * o[1] + t[7] * o[4] + t[8] * o[7];

        int n13 = t[0] * o[2] + t[1] * o[5] + t[2] * o[8];
        int n23 = t[3] * o[2] + t[4] * o[5] + t[5] * o[8];
        int n33 = t[6] * o[2] + t[7] * o[5] + t[8] * o[8];

        return new int[] { n11, n12, n13, n21, n22, n23, n31, n32, n33 };
    }

    public static Rotation of(BlockRotation rotation, Direction.Axis axis)
    {
        if (rotation == BlockRotation.NONE)
        {
            return NONE;
        }

        switch (axis)
        {
            case X:
                switch (rotation)
                {
                    case CW_90:     return ROT_X_90;
                    case CW_180:    return ROT_X_180;
                    case CCW_90:    return ROT_X_270;
                }
                break;
            case Y:
                switch (rotation)
                {
                    case CW_90:     return ROT_Y_90;
                    case CW_180:    return ROT_Y_180;
                    case CCW_90:    return ROT_Y_270;
                }
                break;
            case Z:
                switch (rotation)
                {
                    case CW_90:     return ROT_Z_90;
                    case CW_180:    return ROT_Z_180;
                    case CCW_90:    return ROT_Z_270;
                }
                break;
        }

        return NONE;
    }

    public static Rotation of(BlockMirror mirror)
    {
        if (mirror == BlockMirror.NONE)
        {
            return NONE;
        }

        switch (mirror)
        {
            case X: return MIRROR_X;
            case Y: return MIRROR_Y;
            case Z: return MIRROR_Z;
        }

        return NONE;
    }
}
