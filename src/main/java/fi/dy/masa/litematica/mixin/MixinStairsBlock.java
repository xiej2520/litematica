package fi.dy.masa.litematica.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.StairShape;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.litematica.config.Configs;

import static net.minecraft.block.StairsBlock.FACING;
import static net.minecraft.block.StairsBlock.SHAPE;

@Mixin(StairsBlock.class)
public abstract class MixinStairsBlock extends Block
{
    public MixinStairsBlock(Settings settings)
    {
        super(settings);
    }

    @Inject(method = "mirror", at = @At(value = "HEAD"), cancellable = true)
    private void litematica_fixStairsMirror(BlockState state, BlockMirror mirror, CallbackInfoReturnable<BlockState> cir)
    {
        if (Configs.Generic.FIX_STAIRS_MIRROR.getBooleanValue())
        {
            Direction direction = state.get(FACING);
            StairShape stairShape = state.get(SHAPE);

            // Fixes X Axis for FRONT_BACK being inverted for INNER_LEFT / INNER_RIGHT
            if (direction.getAxis() == Direction.Axis.X && mirror == BlockMirror.FRONT_BACK)
            {
                switch (stairShape)
                {
                    case INNER_LEFT:  cir.setReturnValue(state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.INNER_RIGHT)); break;
                    case INNER_RIGHT: cir.setReturnValue(state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.INNER_LEFT)); break;
                    case OUTER_LEFT:  cir.setReturnValue(state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.OUTER_RIGHT)); break;
                    case OUTER_RIGHT: cir.setReturnValue(state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.OUTER_LEFT)); break;
                    default:          cir.setReturnValue(state.rotate(BlockRotation.CLOCKWISE_180)); break;
                }

                cir.cancel();
            }
            // Fixes missing Axis STAIR_SHAPE flips
            else if ((direction.getAxis() == Direction.Axis.X && mirror == BlockMirror.LEFT_RIGHT) ||
                     (direction.getAxis() == Direction.Axis.Z && mirror == BlockMirror.FRONT_BACK))
            {
                switch (stairShape)
                {
                    case INNER_LEFT:  cir.setReturnValue(state.with(SHAPE, StairShape.INNER_RIGHT)); break;
                    case INNER_RIGHT: cir.setReturnValue(state.with(SHAPE, StairShape.INNER_LEFT)); break;
                    case OUTER_LEFT:  cir.setReturnValue(state.with(SHAPE, StairShape.OUTER_RIGHT)); break;
                    case OUTER_RIGHT: cir.setReturnValue(state.with(SHAPE, StairShape.OUTER_LEFT)); break;
                    default:          cir.setReturnValue(state); break;
                }

                cir.cancel();
            }
        }
    }
}
