package litematica.render.schematic;

import malilib.render.buffer.VertexBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

public class BlockFluidRendererSchematic extends BlockFluidRenderer  {
    private final BlockColors blockColors;
    private TextureAtlasSprite[] atlasSpritesLava;
    private TextureAtlasSprite[] atlasSpritesWater;
    private TextureAtlasSprite atlasSpriteWaterOverlay;

    public BlockFluidRendererSchematic(BlockColors blockColorsIn)
    {
        super(blockColorsIn);
        this.blockColors = blockColorsIn;
        this.initAtlasSprites();
    }

    protected void initAtlasSprites()
    {
        TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();
        this.atlasSpritesLava = new TextureAtlasSprite[] {
            textureMap.getAtlasSprite("minecraft:blocks/lava_still"),
            textureMap.getAtlasSprite("minecraft:blocks/lava_flow"),
        };
        this.atlasSpritesWater = new TextureAtlasSprite[] {
                textureMap.getAtlasSprite("minecraft:blocks/water_still"),
                textureMap.getAtlasSprite("minecraft:blocks/water_flow"),
        };
        this.atlasSpriteWaterOverlay = textureMap.getAtlasSprite("minecraft:blocks/water_overlay");
    }

    public boolean renderFluid(IBlockAccess blockAccess, IBlockState blockStateIn, BlockPos blockPosIn,
                                VertexBuilder builder)
    {
        BlockLiquid blockLiquid = (BlockLiquid) blockStateIn.getBlock();
        boolean isLava = blockStateIn.getMaterial() == Material.LAVA;
        TextureAtlasSprite[] textureAtlasSprites = isLava ? this.atlasSpritesLava : this.atlasSpritesWater;
        int color = this.blockColors.colorMultiplier(blockStateIn, blockAccess, blockPosIn, 0);
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        boolean renderUp = blockStateIn.shouldSideBeRendered(blockAccess, blockPosIn, EnumFacing.UP);
        boolean renderDown = blockStateIn.shouldSideBeRendered(blockAccess, blockPosIn, EnumFacing.DOWN);
        boolean[] renderSides = new boolean[] {
                blockStateIn.shouldSideBeRendered(blockAccess, blockPosIn, EnumFacing.NORTH),
                blockStateIn.shouldSideBeRendered(blockAccess, blockPosIn, EnumFacing.SOUTH),
                blockStateIn.shouldSideBeRendered(blockAccess, blockPosIn, EnumFacing.WEST),
                blockStateIn.shouldSideBeRendered(blockAccess, blockPosIn, EnumFacing.EAST)
        };
        if (!renderUp && !renderDown && !renderSides[0] && !renderSides[1] && !renderSides[2] && !renderSides[3])
        {
            return false;
        }
        else
        {
            boolean renderedSomething = false;
            Material material = blockStateIn.getMaterial();
            float fluidHeight = this.getFluidHeight(blockAccess, blockPosIn, material);
            float fluidHeightSouth = this.getFluidHeight(blockAccess, blockPosIn.south(), material);
            float fluidHeightSouthEast = this.getFluidHeight(blockAccess, blockPosIn.east().south(), material);
            float fluidHeightEast = this.getFluidHeight(blockAccess, blockPosIn.east(), material);
            double x = blockPosIn.getX() & 0xF;
            double y = blockPosIn.getY() & 0xF;
            double z = blockPosIn.getZ() & 0xF;
            if (renderUp)
            {
                renderedSomething = true;
                float t = BlockLiquid.getSlopeAngle(blockAccess, blockPosIn, material, blockStateIn);
                TextureAtlasSprite textureAtlasSprite = t > -999.0F ? textureAtlasSprites[1] : textureAtlasSprites[0];
                float s = 0.001F;
                fluidHeight -= s;
                fluidHeightSouth -= s;
                fluidHeightSouthEast -= s;
                fluidHeightEast -= s;
                float uMin0;
                float uMin1;
                float uMax0;
                float uMax1;
                float vMin0;
                float vMax0;
                float vMax1;
                float vMin1;
                if (t < -999.0F)
                {
                    uMin0 = textureAtlasSprite.getInterpolatedU(0.0F);
                    vMin0 = textureAtlasSprite.getInterpolatedV(0.0F);
                    uMin1 = uMin0;
                    vMax0 = textureAtlasSprite.getInterpolatedV(16.0F);
                    uMax0 = textureAtlasSprite.getInterpolatedU(16.0F);
                    vMax1 = vMax0;
                    uMax1 = uMax0;
                    vMin1 = vMin0;
                }
                else
                {
                    float sin = MathHelper.sin(t) * 0.25F;
                    float cos = MathHelper.cos(t) * 0.25F;
                    float center = 8.0F;
                    uMin0 = textureAtlasSprite.getInterpolatedU((center + (-cos - sin) * 16.0F));
                    vMin0 = textureAtlasSprite.getInterpolatedV((center + (-cos + sin) * 16.0F));
                    uMin1 = textureAtlasSprite.getInterpolatedU((center + (-cos + sin) * 16.0F));
                    vMax0 = textureAtlasSprite.getInterpolatedV((center + (cos + sin) * 16.0F));
                    uMax0 = textureAtlasSprite.getInterpolatedU((center + (cos + sin) * 16.0F));
                    vMax1 = textureAtlasSprite.getInterpolatedV((center + (cos - sin) * 16.0F));
                    uMax1 = textureAtlasSprite.getInterpolatedU((center + (cos - sin) * 16.0F));
                    vMin1 = textureAtlasSprite.getInterpolatedV((center + (-cos - sin) * 16.0F));
                }

                int packedLightmapCoords = blockStateIn.getPackedLightmapCoords(blockAccess, blockPosIn);
                int skyLight = packedLightmapCoords >> 16 & 0xFFFF;
                int blockLight = packedLightmapCoords & 0xFFFF;

                this.addBlockVertex(builder, x + 0, y + fluidHeight, z + 0, r, g, b, 1.0F, uMin0, vMin0, skyLight, blockLight);
                this.addBlockVertex(builder, x + 0, y + fluidHeightSouth, z + 1, r, g, b, 1.0F, uMin1, vMax0, skyLight, blockLight);
                this.addBlockVertex(builder, x + 1, y + fluidHeightSouthEast, z + 1, r, g, b, 1.0F, uMax0, vMax1, skyLight, blockLight);
                this.addBlockVertex(builder, x + 1, y + fluidHeightEast, z + 0, r, g, b, 1.0F, uMax1, vMin1, skyLight, blockLight);

                if (blockLiquid.shouldRenderSides(blockAccess, blockPosIn.up()))
                {
                    this.addBlockVertex(builder, x + 0, y + fluidHeight, z + 0, r, g, b, 1.0F, uMin0, vMin0, skyLight, blockLight);
                    this.addBlockVertex(builder, x + 1, y + fluidHeightEast, z + 0, r, g, b, 1.0F, uMax1, vMin1, skyLight, blockLight);
                    this.addBlockVertex(builder, x + 1, y + fluidHeightSouthEast, z + 1, r, g, b, 1.0F, uMax0, vMax1, skyLight, blockLight);
                    this.addBlockVertex(builder, x + 0, y + fluidHeightSouth, z + 1, r, g, b, 1.0F, uMin1, vMax0, skyLight, blockLight);
                }
            }

            if (renderDown)
            {
                float minU = textureAtlasSprites[0].getMinU();
                float maxU = textureAtlasSprites[0].getMaxU();
                float minV = textureAtlasSprites[0].getMinV();
                float maxV = textureAtlasSprites[0].getMaxV();
                int packedLightmapCoords = blockStateIn.getPackedLightmapCoords(blockAccess, blockPosIn.down());
                int skyLight = packedLightmapCoords >> 16 & 0xFFFF;
                int blockLight = packedLightmapCoords & 0xFFFF;
                this.addBlockVertex(builder, x + 0, y, z + 1, 0.5F, 0.5F, 0.5F, 1.0F, minU, maxV, skyLight, blockLight);
                this.addBlockVertex(builder, x + 0, y, z + 0, 0.5F, 0.5F, 0.5F, 1.0F, minU, minV, skyLight, blockLight);
                this.addBlockVertex(builder, x + 1, y, z + 0, 0.5F, 0.5F, 0.5F, 1.0F, maxU, minV, skyLight, blockLight);
                this.addBlockVertex(builder, x + 1, y, z + 1, 0.5F, 0.5F, 0.5F, 1.0F, maxU, maxV, skyLight, blockLight);
                renderedSomething = true;
            }

            for (int side = 0; side < 4; ++side)
            {
                int xOffset = 0;
                int zOffset = 0;
                if (side == 0)
                {
                    --zOffset;
                }

                if (side == 1)
                {
                    ++zOffset;
                }

                if (side == 2)
                {
                    --xOffset;
                }

                if (side == 3)
                {
                    ++xOffset;
                }

                BlockPos blockPos = blockPosIn.add(xOffset, 0, zOffset);
                TextureAtlasSprite textureAtlasSprite2 = textureAtlasSprites[1];
                if (!isLava)
                {
                    Block block = blockAccess.getBlockState(blockPos).getBlock();
                    if (block == Blocks.GLASS || block == Blocks.STAINED_GLASS)
                    {
                        textureAtlasSprite2 = this.atlasSpriteWaterOverlay;
                    }
                }

                if (renderSides[side])
                {
                    float nearHeight;
                    float farHeight;
                    double x0;
                    double z0;
                    double x1;
                    double z1;
                    if (side == 0)
                    {
                        nearHeight = fluidHeight;
                        farHeight = fluidHeightEast;
                        x0 = x;
                        x1 = x + 1.0;
                        z0 = z + 0.001;
                        z1 = z + 0.001;
                    }
                    else if (side == 1)
                    {
                        nearHeight = fluidHeightSouthEast;
                        farHeight = fluidHeightSouth;
                        x0 = x + 1.0;
                        x1 = x;
                        z0 = z + 1.0 - 0.001;
                        z1 = z + 1.0 - 0.001;
                    }
                    else if (side == 2)
                    {
                        nearHeight = fluidHeightSouth;
                        farHeight = fluidHeight;
                        x0 = x + 0.001;
                        x1 = x + 0.001;
                        z0 = z + 1.0;
                        z1 = z;
                    }
                    else
                    {
                        nearHeight = fluidHeightEast;
                        farHeight = fluidHeightSouthEast;
                        x0 = x + 1.0 - 0.001;
                        x1 = x + 1.0  - 0.001;
                        z0 = z;
                        z1 = z + 1.0;
                    }

                    renderedSomething = true;
                    float uMin = textureAtlasSprite2.getInterpolatedU(0.0F);
                    float uMax = textureAtlasSprite2.getInterpolatedU(8.0F);
                    float vNear = textureAtlasSprite2.getInterpolatedV(((1.0F - nearHeight) * 16.0F * 0.5F));
                    float vFar = textureAtlasSprite2.getInterpolatedV(((1.0F - farHeight) * 16.0F * 0.5F));
                    float vBottom = textureAtlasSprite2.getInterpolatedV(8.0F);
                    int packedLightmapCoords = blockStateIn.getPackedLightmapCoords(blockAccess, blockPos);
                    int skyLight = packedLightmapCoords >> 16 & 0xFFFF;
                    int blockLight = packedLightmapCoords & 0xFFFF;
                    float sc = side < 2 ? 0.8F : 0.6F;
                    float sr = 1.0F * sc * r;
                    float sg = 1.0F * sc * g;
                    float sb = 1.0F * sc * b;

                    this.addBlockVertex(builder, x0, y + nearHeight, z0, sr, sg, sb, 1.0F, uMin, vNear, skyLight, blockLight);
                    this.addBlockVertex(builder, x1, y + farHeight, z1, sr, sg, sb, 1.0F, uMax, vFar, skyLight, blockLight);
                    this.addBlockVertex(builder, x1, y, z1, sr, sg, sb, 1.0F, uMax, vBottom, skyLight, blockLight);
                    this.addBlockVertex(builder, x0, y, z0, sr, sg, sb, 1.0F, uMin, vBottom, skyLight, blockLight);

                    if (textureAtlasSprite2 != this.atlasSpriteWaterOverlay)
                    {
                        this.addBlockVertex(builder, x0, y + 0, z0, sr, sg, sb, 1.0F, uMin, vBottom, skyLight, blockLight);
                        this.addBlockVertex(builder, x1, y + 0, z1, sr, sg, sb, 1.0F, uMax, vBottom, skyLight, blockLight);
                        this.addBlockVertex(builder, x1, y + farHeight, z1, sr, sg, sb, 1.0F, uMax, vFar, skyLight, blockLight);
                        this.addBlockVertex(builder, x0, y + nearHeight, z0, sr, sg, sb, 1.0F, uMin, vNear, skyLight, blockLight);
                    }
                }
            }

            return renderedSomething;
        }
    }

    private float getFluidHeight(IBlockAccess blockAccess, BlockPos blockPosIn, Material blockMaterial)
    {
        int i = 0;
        float f = 0.0F;

        for (int j = 0; j < 4; ++j)
        {
            BlockPos blockPos = blockPosIn.add(-(j & 1), 0, -(j >> 1 & 1));
            if (blockAccess.getBlockState(blockPos.up()).getMaterial() == blockMaterial)
            {
                return 1.0F;
            }

            IBlockState blockState = blockAccess.getBlockState(blockPos);
            Material material = blockState.getMaterial();
            if (material == blockMaterial)
            {
                int k = blockState.getValue(BlockLiquid.LEVEL);
                if (k >= 8 || k == 0)
                {
                    f += BlockLiquid.getLiquidHeightPercent(k) * 10.0F;
                    i += 10;
                }

                f += BlockLiquid.getLiquidHeightPercent(k);
                ++i;
            }
            else if (!material.isSolid())
            {
                ++f;
                ++i;
            }
        }

        return 1.0F - f / (float)i;
    }

    private void addBlockVertex(VertexBuilder buffer, double x, double y, double z, float r, float g, float b, float a, float u, float v, int skyLight, int blockLight)
    {
        int ri = (int) (r * 255f);
        int gi = (int) (g * 255f);
        int bi = (int) (b * 255f);
        int ai = (int) (a * 255f);
        buffer.addVertexData(new int[]
            {
                Float.floatToRawIntBits((float) x),
                Float.floatToRawIntBits((float) y),
                Float.floatToRawIntBits((float) z),
                (ri << 24) | (gi << 16) | (bi << 8) | ai,
                Float.floatToRawIntBits(u),
                Float.floatToRawIntBits(v),
                (skyLight << 16) | blockLight,
            }
        );
    }
}
