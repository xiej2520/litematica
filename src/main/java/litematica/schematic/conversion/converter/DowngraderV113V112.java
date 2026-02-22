package litematica.schematic.conversion.converter;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import litematica.Litematica;
import litematica.schematic.container.ArrayBlockContainer;
import litematica.schematic.conversion.SchematicDataConverter;
import litematica.schematic.data.EntityData;
import malilib.gui.BaseScreen;
import malilib.overlay.message.MessageDispatcher;
import malilib.util.data.Constants;
import malilib.util.data.tag.CompoundData;
import malilib.util.data.tag.ListData;
import malilib.util.game.BlockUtils;
import malilib.util.game.MinecraftVersion;
import malilib.util.position.BlockPos;
import malilib.util.world.BlockState;
import malilib.util.world.ScheduledBlockTickData;
import net.minecraft.init.Blocks;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class DowngraderV113V112 extends SchematicDataConverter implements MiniDataConverter
{
    public static DowngraderV113V112 INSTANCE = new DowngraderV113V112();

    protected Map<CompoundData, CompoundData> stateMap = new HashMap<>();
    Map<String, ItemIdDamage> itemMap = new HashMap<>();

    public static MinecraftVersion versionFrom = MinecraftVersion.MC_1_13;
    public static MinecraftVersion versionTo = MinecraftVersion.MC_1_12;

    protected DowngraderV113V112()
    {
        Optional<Map<CompoundData, CompoundData>> stateMap = BlockStateMapReader.readMap("block_state_map_113_to_112.json", "1.13", "1.12");
        if (stateMap.isPresent())
        {
            this.stateMap = stateMap.get();
        }
        else
        {
            MessageDispatcher.error("failed to read block_state_map_113_to_112.json");
        }

        Optional<Map<CompoundData, CompoundData>> itemTagMap = ItemMapReader.readMap("item_map_113_to_112.ndjson", "1.13", "1.12");
        if (itemTagMap.isPresent())
        {
            for (Map.Entry<CompoundData, CompoundData> entry : itemTagMap.get().entrySet())
            {
                String id113 = entry.getKey().getString("id");

                CompoundData tag112 = entry.getValue();
                ItemIdDamage idDamage112 = new ItemIdDamage(tag112.getString("id"), tag112.getShort("Damage"));
                this.itemMap.put(id113, idDamage112);
            }
        }
        else
        {
            MessageDispatcher.error("failed to read item_map_113_to_112.json");
        }
    }


    /// BlockState downgrade converter
    /// right inverse function: if a blockstate exists in 1.13, and a 1.12 blockstate gets converted
    /// to it by vanilla datafixer, then this converter should try to restore the 1.13 blockstate to
    /// the 1.12 blockstate. If multiple 1.12 blockstates map to the 1.13 state, pick the most reasonable one.
    ///
    /// duplicates from merges:
    /// dirt/coarse dirt, flowing water/lava, leaves, shrub (tallgrass 31:0), double stone slab,
    /// smooth_stone, smooth (red)sandstone, smooth quartz, mushroom blocks, pumpkin/melon stem
    /// flower pot, skull, powered redstone comparator, double_plant
    public void convertContainer(
        ListData paletteTag,
        ArrayBlockContainer container,
        Map<BlockPos, CompoundData> blockEntityMap,
        Map<BlockPos, ScheduledBlockTickData> blockTickMap
    ) {
        final int paletteSize = paletteTag.size();
        //ListData paletteTagOut = new ListData(Constants.NBT.TAG_COMPOUND);

        ArrayList<String> failedStates = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        ListData paletteTagOriginal = paletteTag.copy();
        boolean needBlockFixer = false;

        for (int i = 0; i < paletteSize; ++i)
        {
            CompoundData tag = paletteTag.getCompoundAt(i);
            String flattenedBlockName = tag.getString("Name");

            if (this.convertBlockStateData(tag)) {
                System.out.printf("converted: %s => %s\n", paletteTagOriginal.getCompoundAt(i), tag);
                if (unfixers.containsKey(flattenedBlockName)) {
                    needBlockFixer = true;
                }
                successCount += 1;
            } else {
                System.out.printf("FAILED: %s\n", tag);
                failedStates.add(tag.toString());
                paletteTag.set(i, BlockUtils.writeBlockState(new CompoundData(), BlockState.of(Blocks.BARRIER.getDefaultState())));
                failCount += 1;
            }
        }

        // TODO fix duplicate blockstates in palette from downgrade, e.g. merge "flower_pot"s

        if (needBlockFixer) {
            for (int x = 0; x < container.getSize().getX(); x++) {
                for (int y = 0; y < container.getSize().getY(); y++) {
                    for (int z = 0; z < container.getSize().getZ(); z++) {
                        int i = container.getPaletteId(x, y, z);
                        CompoundData tag = paletteTagOriginal.getCompoundAt(i);
                        String blockName = tag.getString("Name");
                        UnfixBlockEntityCreator fixer = unfixers.get(blockName);
                        if (fixer != null) {
                            fixer.recreateBlockEntity(new BlockPos(x, y, z), container, blockEntityMap, tag);
                        }
                    }
                }
            }
        }

        this.convertBlockEntities(blockEntityMap);

        if (failCount > 0)
        {
            String verFrom = versionFrom.displayName;
            String verTo = versionTo.displayName;
            String strSu = String.valueOf(successCount);
            String strFa = String.valueOf(failCount);
            MessageDispatcher.warning("litematica.message.warn.schematic_conversion.palette_conversion_failures", verFrom, verTo, strSu, strFa);
            MessageDispatcher.error(String.join("\n", failedStates));
            BaseScreen.openPopupScreen(new SaveConversionFailureLogScreen(failedStates));
        }
    }

    public boolean convertBlockStateData(CompoundData data) {
        CompoundData convertedData = this.stateMap.get(data);
        if (convertedData == null) {
            return false;
        }
        data.putString("Name", convertedData.getString("Name"));
        data.remove("Properties");
        if (convertedData.contains("Properties", Constants.NBT.TAG_COMPOUND)) {
            data.put("Properties", convertedData.getCompound("Properties").copy());
        }
        return true;
    }

    public void convertEntityList(List<EntityData> entityList)
    {
        // TODO: painting Motive remap
        //   "donkeykong" <- "donkey_kong"; "burningskull" <- "burning_skull; "skullandroses" <- "skull_and_roses"

        for (EntityData entityData : entityList) {
            convertEntityData(entityData.data);
        }
    }

    public boolean convertEntityData(CompoundData data)
    {
        String id = data.getString("id");
        String unrenamedId = ENTITY_UNRENAME_MAP.get(id);
        if (unrenamedId != null) {
            data.putString("id", unrenamedId);
        }

        if (id.equals("minecraft:item_frame")) { // v1456
            byte facing3d = data.getByte("Facing");
            byte facing2d;
            switch (facing3d) {
                case 3: facing2d = 0; break;
                case 4: facing2d = 1; break;
                case 5: facing2d = 3; break;
                case 2: // fallthrough
                default: facing2d = 2;
            }
            data.putByte("Facing", facing2d);
        }

        if (data.contains("Item", Constants.NBT.TAG_COMPOUND)) {
            convertItemData(data.getCompound("Item"));
        }

        this.convertItemsListIfKey(data, "Items");
        this.convertItemsListIfKey(data, "ArmorItems");
        this.convertItemsListIfKey(data, "HandItems");

        if (data.contains("Offers", Constants.NBT.TAG_COMPOUND)) {
            convertOffers(data.getCompound("Offers"));
        }
        return true;
    }

    private void convertBlockEntities(Map<BlockPos, CompoundData> blockEntityMap)
    {
        for (CompoundData blockEntityTag : blockEntityMap.values())
        {
            convertBlockEntityData(blockEntityTag);
        }
    }

    // TODO: in 1.12, double chests have reversed inventories when facing north or east
    public boolean convertBlockEntityData(CompoundData blockEntityTag)
    {
        String id = blockEntityTag.getString("id");
        switch (id) {
            case "minecraft:beacon":
            case "minecraft:enchanting_table":
                convertNamed(blockEntityTag);
                break;
            case "minecraft:banner":
                convertNamed(blockEntityTag);
                unfixBannerBlockEntity(blockEntityTag);
                blockEntityTag.remove("id"); // not stored in tag in 1.12
                break;
            case "minecraft:brewing_stand":
            case "minecraft:chest":
            case "minecraft:dispenser":
            case "minecraft:dropper":
            case "minecraft:furnace":
            case "minecraft:hopper":
            case "minecraft:shulker_box":
                convertNamedInventory(blockEntityTag);
                break;
            case "minecraft:trapped_chest":
                blockEntityTag.putString("id", "minecraft:chest"); // v1624, 1.13.1
                convertNamedInventory(blockEntityTag);
                break;
            // TODO piston Block 36 blockState -> blockId + blockData
            // TODO Jukebox RecordItem id Count -> Record
            default:
                return true;
        }
        return true;
    }
    
    
    private void convertNamed(CompoundData blockEntityTag)
    {
        // TODO: verify this is correct, from plain text component
        // v1458
        if (blockEntityTag.contains("CustomName", Constants.NBT.TAG_STRING)) {
            JsonObject obj = JsonParser.parseString(blockEntityTag.getString("CustomName")).getAsJsonObject();
            JsonElement textComponent = obj.get("text");
            if (textComponent != null && textComponent.isJsonPrimitive()) {
                String customName = textComponent.getAsString();
                blockEntityTag.putString("CustomName", customName);
            }
        }
    }

    private void convertNamedInventory(CompoundData blockEntityTag)
    {
        // v1458
        convertNamed(blockEntityTag);
        this.convertItemsListIfKey(blockEntityTag, "Items");
    }

    private void convertOffers(CompoundData offers)
    {
        if (offers.containsList("Recipes", Constants.NBT.TAG_COMPOUND)) {
            ListData recipes = offers.getList("Recipes", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < recipes.size(); i++) {
                CompoundData recipe = recipes.getCompoundAt(i);
                if (recipe.contains("buy", Constants.NBT.TAG_COMPOUND)) {
                    convertItemData(recipe.getCompound("buy"));
                }
                if (recipe.contains("sell", Constants.NBT.TAG_COMPOUND)) {
                    convertItemData(recipe.getCompound("sell"));
                }
            }
        }
    }

    public boolean convertItemData(CompoundData itemTag)
    {
        // v1451 flattening item names
        String id13 = itemTag.getString("id");
        ItemIdDamage idDamage = this.itemMap.get(id13);

        // TODO: fallback items
        if (idDamage != null) {
            itemTag.putString("id", idDamage.id);
            itemTag.putShort("Damage", idDamage.damage);
        }

        CompoundData tag = itemTag.getCompound("tag");
        if (tag != null && tag.isEmpty() == false) {
            if (tag.contains("Damage", Constants.NBT.TAG_INT)) {
                short actualDamage = tag.getShort("Damage");
                itemTag.putShort("Damage", actualDamage);
                tag.remove("Damage");
            }

            if (tag.contains("StoredEnchantments", Constants.NBT.TAG_LIST)) { // v1494
                ListData storedEnchantments = tag.getList("StoredEnchantments", Constants.NBT.TAG_LIST);
                convertEnchantments(storedEnchantments);
            }
            if (tag.contains("Enchantments", Constants.NBT.TAG_LIST)) { // v1494
                ListData enchantments = tag.getList("Enchantments", Constants.NBT.TAG_LIST);
                convertEnchantments(enchantments);
                tag.remove("Enchantments");
                tag.put("ench", enchantments);
            }

            // Potion stayed the same (I think?)
            //if (tag.contains("Potion", Constants.NBT.TAG_STRING))

            if (tag.contains("EntityTag", Constants.NBT.TAG_COMPOUND)) {
                CompoundData entityTag = tag.getCompound("EntityTag");
                // TODO: confirm this is correct
                convertEntityData(entityTag);
            }

            if (tag.contains("BlockEntityTag", Constants.NBT.TAG_COMPOUND)) {
                CompoundData blockEntityTag = tag.getCompound("BlockEntityTag");
                convertBlockEntityData(blockEntityTag);
            }

            if (tag.contains("display", Constants.NBT.TAG_COMPOUND)) {
                CompoundData displayTag = tag.getCompound("display");
                if (displayTag.contains("Name", Constants.NBT.TAG_STRING)) {
                    JsonObject obj = JsonParser.parseString(displayTag.getString("Name")).getAsJsonObject();
                    JsonElement textComponent = obj.get("text");
                    if (textComponent != null && textComponent.isJsonPrimitive()) {
                        String customName = textComponent.getAsString();
                        displayTag.putString("Name", customName);
                    }
                }
            }

        }
        if (tag != null && tag.isEmpty())
        {
            itemTag.remove("tag");
        }
        return true;
    }

    // v1494 enchantment id to name
    private void convertEnchantments(ListData storedEnchantments)
    {
        for (int i = 0; i < storedEnchantments.size(); i++) {
            CompoundData enchantmentTag = storedEnchantments.getCompoundAt(i);
            String nameId = enchantmentTag.getString("id");
            enchantmentTag.putShort("id", ENCHANTMENT_ID_TO_NAME.get(nameId).shortValue());
            // keep lvl the same
        }
    }

    static final ImmutableMap<String, Pair<String, Integer>> flowerPotData =
        ImmutableMap.<String, Pair<String, Integer>>builder()
        .put("minecraft:flower_pot",              Pair.of("minecraft:air", 0))
        .put("minecraft:potted_poppy",            Pair.of("minecraft:red_flower", 0))
        .put("minecraft:potted_blue_orchid",      Pair.of("minecraft:red_flower", 1))
        .put("minecraft:potted_allium",           Pair.of("minecraft:red_flower", 2))
        .put("minecraft:potted_azure_bluet",      Pair.of("minecraft:red_flower", 3))
        .put("minecraft:potted_red_tulip",        Pair.of("minecraft:red_flower", 4))
        .put("minecraft:potted_orange_tulip",     Pair.of("minecraft:red_flower", 5))
        .put("minecraft:potted_white_tulip",      Pair.of("minecraft:red_flower", 6))
        .put("minecraft:potted_pink_tulip",       Pair.of("minecraft:red_flower", 7))
        .put("minecraft:potted_oxeye_daisy",      Pair.of("minecraft:red_flower", 8))
        .put("minecraft:potted_dandelion",        Pair.of("minecraft:yellow_flower", 0))
        .put("minecraft:potted_oak_sapling",      Pair.of("minecraft:sapling", 0))
        .put("minecraft:potted_spruce_sapling",   Pair.of("minecraft:sapling", 1))
        .put("minecraft:potted_birch_sapling",    Pair.of("minecraft:sapling", 2))
        .put("minecraft:potted_jungle_sapling",   Pair.of("minecraft:sapling", 3))
        .put("minecraft:potted_acacia_sapling",   Pair.of("minecraft:sapling", 4))
        .put("minecraft:potted_dark_oak_sapling", Pair.of("minecraft:sapling", 5))
        .put("minecraft:potted_brown_mushroom",   Pair.of("minecraft:brown_mushroom", 0))
        .put("minecraft:potted_red_mushroom",     Pair.of("minecraft:red_mushroom", 0))
        .put("minecraft:potted_dead_bush",        Pair.of("minecraft:deadbush", 0))
        .put("minecraft:potted_fern",             Pair.of("minecraft:tallgrass", 2))
        .put("minecraft:potted_cactus",           Pair.of("minecraft:cactus", 0))
        .build();

    static final String[] colorId = new String[] {
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    static final Map<String, Integer> bedColor = new HashMap<>();
    static final Map<String, Integer> bannerColor = new HashMap<>();
    static {
        for (int i = 0; i < 16; i++)
        {
            bedColor.put("minecraft:" + colorId[i] + "_bed", i);
            // banner color ids are the metadata, which is reversed
            bannerColor.put("minecraft:" + colorId[i] + "_banner", 15 - i);
            bannerColor.put("minecraft:" + colorId[i] + "_wall_banner", 15 - i);
        }
    }
    static final ImmutableMap<String, Integer> skullTypes = ImmutableMap.<String, Integer>builder()
        .put("minecraft:skeleton_skull",             0)
        .put("minecraft:skeleton_wall_skull",        0)
        .put("minecraft:wither_skeleton_skull",      1)
        .put("minecraft:wither_skeleton_wall_skull", 1)
        .put("minecraft:zombie_head",                2)
        .put("minecraft:zombie_wall_head",           2)
        .put("minecraft:player_head",                3)
        .put("minecraft:player_wall_head",           3)
        .put("minecraft:creeper_head",               4)
        .put("minecraft:creeper_wall_head",          4)
        .put("minecraft:dragon_head",                5)
        .put("minecraft:dragon_wall_head",           5)
        .build();

    static final UnfixBlockEntityCreator UNFIX_FLOWERPOT = (pos, container, blockEntityMap, originalTag) -> {
        Pair<String, Integer> itemData = flowerPotData.get(originalTag.getString("Name"));

        CompoundData blockEntityTag = new CompoundData();
        blockEntityTag.putString("Item", itemData.getLeft());
        blockEntityTag.putInt("Data", itemData.getRight());
        blockEntityTag.putString("id", "minecraft:flower_pot");
        blockEntityTag.putInt("x", pos.getX());
        blockEntityTag.putInt("y", pos.getY());
        blockEntityTag.putInt("z", pos.getZ());
        blockEntityMap.put(pos, blockEntityTag);
    };

    static final UnfixBlockEntityCreator UNFIX_NOTE_BLOCK = (pos, container, blockEntityMap, originalTag) -> {
        CompoundData properties = originalTag.getCompound("Properties");
        if (properties != null)
        {
            byte note = 0;
            boolean powered = Boolean.parseBoolean(properties.getString("powered"));
            try {
                note = Byte.parseByte(properties.getString("note"));
            } catch (NumberFormatException e) {
                Litematica.LOGGER.error(e);
            }
            // instrument is not stored in 1.12

            CompoundData blockEntityTag = new CompoundData();
            blockEntityTag.putByte("note", note);
            blockEntityTag.putBoolean("powered", powered);
            blockEntityTag.putString("id", "minecraft:noteblock");
            blockEntityTag.putInt("x", pos.getX());
            blockEntityTag.putInt("y", pos.getY());
            blockEntityTag.putInt("z", pos.getZ());
            blockEntityMap.put(pos, blockEntityTag);
        }
    };

    static final UnfixBlockEntityCreator UNFIX_BED = (pos, container, blockEntityMap, originalTag) -> {
        int color = bedColor.getOrDefault(originalTag.getString("Name"), 0);

        CompoundData blockEntityTag = new CompoundData();
        blockEntityTag.putInt("color", color);
        blockEntityTag.putString("id", "minecraft:bed");
        blockEntityTag.putInt("x", pos.getX());
        blockEntityTag.putInt("y", pos.getY());
        blockEntityTag.putInt("z", pos.getZ());
        blockEntityMap.put(pos, blockEntityTag);
    };

    // banner needs to get it's Base color id using the container
    // but the block entity should already exist in the schematic, recreate it to be safe
    static final UnfixBlockEntityCreator UNFIX_BANNER = (pos, container, blockEntityMap, originalTag) -> {
        CompoundData blockEntityTag = blockEntityMap.getOrDefault(pos, new CompoundData());
        // black by default
        blockEntityTag.putString("id", "minecraft:banner");

        // do not run unfixBannerBlockEntity(blockEntityTag) here, run it in the block entity converter

        int colorMetadata = bannerColor.getOrDefault(originalTag.getString("Name"), 15);
        blockEntityTag.putInt("Base", colorMetadata);
        blockEntityTag.putInt("x", pos.getX());
        blockEntityTag.putInt("y", pos.getY());
        blockEntityTag.putInt("z", pos.getZ());
        blockEntityMap.put(pos, blockEntityTag);
    };

    static void unfixBannerBlockEntity(CompoundData blockEntityTag)
    {
        if (blockEntityTag.containsList("Patterns", Constants.NBT.TAG_COMPOUND)) {
            ListData patterns = blockEntityTag.getList("Patterns", Constants.NBT.TAG_COMPOUND);
            if (patterns.size() > 0)
            {
                for (int i = 0; i < patterns.size(); i++)
                {
                    CompoundData pattern = patterns.getCompoundAt(i);
                    // 1.12 stores metadata color value, 1.13 stores id color value
                    pattern.putInt("Color", 15 - pattern.getIntOrDefault("Color", 0));
                }
            }
            else
            {
                blockEntityTag.remove("Patterns");
            }
        }
        else
        {
            blockEntityTag.remove("Patterns");
        }
    }

    static final UnfixBlockEntityCreator UNFIX_SKULL = (pos, container, blockEntityMap, originalTag) -> {
        CompoundData blockEntityTag = new CompoundData();

        CompoundData flattenedBlockEntityTag = blockEntityMap.get(pos);
        if (flattenedBlockEntityTag != null)
        {
            CompoundData owner = flattenedBlockEntityTag.getCompound("Owner");
            if (owner != null && owner.isEmpty() == false) {
                blockEntityTag.put("Owner", owner);
            }
        }

        byte rotationByte = 0;
        CompoundData properties = originalTag.getCompound("Properties");
        if (properties != null)
        {
            String facing = originalTag.getString("facing");
            switch (facing) {
                case "north": rotationByte = 2; break;
                case "south": rotationByte = 3; break;
                case "west":  rotationByte = 4; break;
                case "east":  rotationByte = 5; break;
            }

            String rotation = properties.getString("rotation");
            if (rotation != null && rotation.isEmpty() == false)
            {
                try {
                    rotationByte = Byte.parseByte(properties.getString("rotation"));
                } catch (NumberFormatException e) {
                    Litematica.LOGGER.error(e);
                }
            }
        }

        blockEntityTag.putByte("Rot", rotationByte);
        blockEntityTag.putString("id", "minecraft:skull");
        blockEntityTag.putByte("SkullType", skullTypes.getOrDefault(originalTag.getString("Name"), 0).byteValue());
        blockEntityTag.putInt("x", pos.getX());
        blockEntityTag.putInt("y", pos.getY());
        blockEntityTag.putInt("z", pos.getZ());
        blockEntityMap.put(pos, blockEntityTag);
    };

    static final Map<String, UnfixBlockEntityCreator> unfixers = new HashMap<>();
    static {
        unfixers.put("minecraft:flower_pot",              UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_poppy",            UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_blue_orchid",      UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_allium",           UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_azure_bluet",      UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_red_tulip",        UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_orange_tulip",     UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_white_tulip",      UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_pink_tulip",       UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_oxeye_daisy",      UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_dandelion",        UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_oak_sapling",      UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_spruce_sapling",   UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_birch_sapling",    UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_jungle_sapling",   UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_acacia_sapling",   UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_dark_oak_sapling", UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_brown_mushroom",   UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_red_mushroom",     UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_dead_bush",        UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_fern",             UNFIX_FLOWERPOT);
        unfixers.put("minecraft:potted_cactus",           UNFIX_FLOWERPOT);

        unfixers.put("minecraft:note_block", UNFIX_NOTE_BLOCK);

        for (String color : colorId)
        {
            unfixers.put("minecraft:" + color + "_bed", UNFIX_BED);
            unfixers.put("minecraft:" + color + "_banner",      UNFIX_BANNER);
            unfixers.put("minecraft:" + color + "_wall_banner", UNFIX_BANNER);
        }
        unfixers.put("minecraft:skeleton_skull",             UNFIX_SKULL);
        unfixers.put("minecraft:skeleton_wall_skull",        UNFIX_SKULL);
        unfixers.put("minecraft:wither_skeleton_skull",      UNFIX_SKULL);
        unfixers.put("minecraft:wither_skeleton_wall_skull", UNFIX_SKULL);
        unfixers.put("minecraft:player_head",                UNFIX_SKULL);
        unfixers.put("minecraft:player_wall_head",           UNFIX_SKULL);
        unfixers.put("minecraft:zombie_head",                UNFIX_SKULL);
        unfixers.put("minecraft:zombie_wall_head",           UNFIX_SKULL);
        unfixers.put("minecraft:creeper_head",               UNFIX_SKULL);
        unfixers.put("minecraft:creeper_wall_head",          UNFIX_SKULL);
        unfixers.put("minecraft:dragon_head",                UNFIX_SKULL);
        unfixers.put("minecraft:dragon_wall_head",           UNFIX_SKULL);
    }

    // v1494
    static final ImmutableMap<String, Integer> ENCHANTMENT_ID_TO_NAME = ImmutableMap.<String, Integer>builder()
        .put("minecraft:protection",            0)
        .put("minecraft:fire_protection",       1)
        .put("minecraft:feather_falling",       2)
        .put("minecraft:blast_protection",      3)
        .put("minecraft:projectile_protection", 4)
        .put("minecraft:respiration",           5)
        .put("minecraft:aqua_affinity",         6)
        .put("minecraft:thorns",                7)
        .put("minecraft:depth_strider",         8)
        .put("minecraft:frost_walker",          9)
        .put("minecraft:binding_curse",         10)
        .put("minecraft:sharpness",             16)
        .put("minecraft:smite",                 17)
        .put("minecraft:bane_of_arthropods",    18)
        .put("minecraft:knockback",             19)
        .put("minecraft:fire_aspect",           20)
        .put("minecraft:looting",               21)
        .put("minecraft:sweeping",              22)
        .put("minecraft:efficiency",            32)
        .put("minecraft:silk_touch",            33)
        .put("minecraft:unbreaking",            34)
        .put("minecraft:fortune",               35)
        .put("minecraft:power",                 48)
        .put("minecraft:punch",                 49)
        .put("minecraft:flame",                 50)
        .put("minecraft:infinity",              51)
        .put("minecraft:luck_of_the_sea",       61)
        .put("minecraft:lure",                  62)
        .put("minecraft:loyalty",               65)
        .put("minecraft:impaling",              66)
        .put("minecraft:riptide",               67)
        .put("minecraft:channeling",            68)
        .put("minecraft:mending",               70)
        .put("minecraft:vanishing_curse",       71)
        .build();

    // v1510
    static final ImmutableMap<String, String> ENTITY_UNRENAME_MAP = ImmutableMap.<String, String>builder()
        .put("minecraft:command_block_minecart", "minecraft:commandblock_minecart")
        .put("minecraft:end_crystal", "minecraft:ender_crystal")
        .put("minecraft:snow_golem", "minecraft:snowman")
        .put("minecraft:evoker", "minecraft:evocation_illager")
        .put("minecraft:evoker_fangs", "minecraft:evocation_fangs")
        .put("minecraft:illusioner", "minecraft:illusion_illager")
        .put("minecraft:vindicator", "minecraft:vindication_illager")
        .put("minecraft:iron_golem", "minecraft:villager_golem")
        .put("minecraft:experience_orb", "minecraft:xp_orb")
        .put("minecraft:experience_bottle", "minecraft:xp_bottle")
        .put("minecraft:eye_of_ender", "minecraft:eye_of_ender_signal")
        .put("minecraft:firework_rocket", "minecraft:fireworks_rocket")
        .build();
}

interface UnfixBlockEntityCreator {
    void recreateBlockEntity(
        BlockPos pos,
        ArrayBlockContainer container,
        Map<BlockPos, CompoundData> blockEntityMap,
        CompoundData originalTag
    );
}

class ItemIdDamage {
    String id;
    short damage;
    public ItemIdDamage(String id, short damage)
    {
        this.id = id;
        this.damage = damage;
    }
}

