package fi.dy.masa.litematica.world;

import java.util.function.BiFunction;

import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccessType;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;

public class PublicDimensionType extends DimensionType
{
    public PublicDimensionType(
            int dimensionId,
            String suffix,
            String saveDir,
            BiFunction<World, DimensionType, ? extends Dimension> factory,
            boolean hasSkylight,
            BiomeAccessType biomeAccessType
    ) {
        super(dimensionId, suffix, saveDir, factory, hasSkylight, biomeAccessType);
    }
}
