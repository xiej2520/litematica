package fi.dy.masa.litematica.world;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.GameMode;
import net.minecraft.world.biome.source.VoronoiBiomeAccessType;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.TheEndDimension;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;
import fi.dy.masa.litematica.Litematica;
import fi.dy.masa.litematica.render.LitematicaRenderer;

public class SchematicWorldHandler
{
    @Nullable private static WorldSchematic world;
    public static final DimensionType DIMENSIONTYPE = new PublicDimensionType(2, DimensionType.THE_END.getSuffix(),
            "DIM1", TheEndDimension::new, false, VoronoiBiomeAccessType.INSTANCE);

    @Nullable
    public static WorldSchematic getSchematicWorld()
    {
        return world;
    }

    @Nullable
    public static WorldSchematic createSchematicWorld()
    {
        if (MinecraftClient.getInstance().world == null)
        {
            return null;
        }

        LevelInfo info = new LevelInfo(0, GameMode.CREATIVE, false, false, LevelGeneratorType.FLAT);
        return new WorldSchematic(new LevelProperties(info, "SchematicWorld"), DIMENSIONTYPE, MinecraftClient.getInstance().getProfiler());
    }

    public static void recreateSchematicWorld(boolean remove)
    {
        if (remove)
        {
            Litematica.debugLog("Removing the schematic world...");
            world = null;
        }
        else
        {
            Litematica.debugLog("(Re-)creating the schematic world...");
            // Note: The dimension used here must have no skylight, because the custom Chunks don't have those arrays
            world = createSchematicWorld();
            Litematica.debugLog("Schematic world (re-)created: {}", world);
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(world);
    }
}
