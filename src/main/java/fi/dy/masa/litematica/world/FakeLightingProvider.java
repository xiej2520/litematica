package fi.dy.masa.litematica.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightingProvider;

import fi.dy.masa.litematica.config.Configs;

public class FakeLightingProvider extends LightingProvider
{
    private final FakeLightingView lightingView;

    public FakeLightingProvider()
    {
        super(null, false, false);

        this.lightingView = new FakeLightingView();
    }

    @Override
    public ChunkLightingView get(LightType type)
    {
        return this.lightingView;
    }

    @Override
    public int getLight(BlockPos pos, int ambientDarkness)
    {
        //return 15;
        return Configs.Visuals.RENDER_FAKE_LIGHTING_LEVEL.getIntegerValue();
    }

    public static class FakeLightingView implements ChunkLightingView
    {
        @Override
        public void updateSectionStatus(ChunkSectionPos var1, boolean var2)
        {
            // NO-OP
        }

        @Override
        public ChunkNibbleArray getLightArray(ChunkSectionPos var1)
        {
            return null;
        }

        @Override
        public int getLightLevel(BlockPos pos)
        {
            //return 15;
            return Configs.Visuals.RENDER_FAKE_LIGHTING_LEVEL.getIntegerValue();
        }
    }
}
