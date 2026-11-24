package fi.dy.masa.litematica.schematic.conversion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import fi.dy.masa.malilib.util.Constants;

// Fixes palette properties before the palette is passed to blockstate parser.
public class SchematicDowngradeFixers
{
    // 1.16 snapshot dataversion where WallPropertyFix changed the wall sides to none/low/tall
    private static final int MINECRAFT_DATA_VERSION_WALL_SHAPES = 2503;
    private static final Set<String> WALL_BLOCKS = new HashSet<>(Arrays.asList(
            "minecraft:andesite_wall",
            "minecraft:brick_wall",
            "minecraft:cobblestone_wall",
            "minecraft:diorite_wall",
            "minecraft:end_stone_brick_wall",
            "minecraft:granite_wall",
            "minecraft:mossy_cobblestone_wall",
            "minecraft:mossy_stone_brick_wall",
            "minecraft:nether_brick_wall",
            "minecraft:prismarine_wall",
            "minecraft:red_nether_brick_wall",
            "minecraft:red_sandstone_wall",
            "minecraft:sandstone_wall",
            "minecraft:stone_brick_wall"
    ));

    private SchematicDowngradeFixers()
    {
    }

    public static ListTag convertBlockStatePalette_1_16_to_1_15(ListTag palette, int minecraftDataVersion)
    {
        if (minecraftDataVersion < MINECRAFT_DATA_VERSION_WALL_SHAPES)
        {
            return palette;
        }

        for (int i = 0; i < palette.size(); ++i)
        {
            CompoundTag state = palette.getCompound(i);

            if (WALL_BLOCKS.contains(state.getString("Name")) &&
                state.contains("Properties", Constants.NBT.TAG_COMPOUND))
            {
                CompoundTag properties = state.getCompound("Properties");

                for (String side : Arrays.asList("north", "south", "east", "west"))
                {
                    String value = properties.getString(side);

                    if ("low".equals(value) || "tall".equals(value))
                    {
                        properties.putString(side, "true");
                    }
                    else if ("none".equals(value))
                    {
                        properties.putString(side, "false");
                    }
                }
            }
        }

        return palette;
    }

    // 1.20+ uses front_text/back_text for sign lines. Move them back to Text1-Text4 and Color,
    // drop other lines. Don't modify `GlowingText`/`has_glowing_text` (handle in 1.17+).
    public static void convertBlockEntities_1_20_to_1_15(Map<?, CompoundTag> blockEntities)
    {
        for (CompoundTag tag : blockEntities.values())
        {
            convertSignText_1_20_to_1_15(tag);
        }
    }

    public static void convertSignText_1_20_to_1_15(CompoundTag tag)
    {
        if (tag.contains("front_text", Constants.NBT.TAG_COMPOUND) == false ||
            tag.contains("Text1", Constants.NBT.TAG_STRING))
        {
            return;
        }

        CompoundTag front = tag.getCompound("front_text");
        ListTag messages = front.getList("messages", Constants.NBT.TAG_STRING);

        for (int i = 0; i < 4; ++i)
        {
            tag.putString("Text" + (i + 1), i < messages.size() ? messages.getString(i) : "\"\"");
        }

        if (front.contains("color", Constants.NBT.TAG_STRING))
        {
            tag.putString("Color", front.getString("color"));
        }

        tag.remove("front_text");
        tag.remove("back_text");
        tag.remove("is_waxed");
    }
}

