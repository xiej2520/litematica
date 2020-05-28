package fi.dy.masa.litematica.schematic.conversion;

import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import fi.dy.masa.litematica.schematic.conversion.converter.BlockEntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.BlockStateConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.BlockTickDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.EntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.EntityDataConverterBase;
import fi.dy.masa.litematica.schematic.conversion.converter.InventoryDataConverter;
import fi.dy.masa.malilib.util.InfoUtils;

public class ConversionUtils
{
    public static NBTTagList convertBlockStatePalette(NBTTagList paletteTag, SchematicDataVersion versionFrom, MinecraftVersion versionTo)
    {
        BlockStateConverter converter = SchematicDataConversionManager.INSTANCE.getBlockStateConverter(versionFrom.getMinecraftVersion(), versionTo);

        if (converter != null)
        {
            paletteTag = converter.convertPalette(paletteTag);
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.error.schematic_conversion.missing_converter.block_states",
                                        versionFrom.getMcVersionDisplayName(), String.valueOf(versionFrom.getDataVersion()), versionTo.getMcVersionDisplayName());
        }

        return paletteTag;
    }

    public static NBTTagList convertBlockEntityData(NBTTagList blockEntityList, String idTagName, SchematicDataVersion versionFrom, MinecraftVersion versionTo)
    {
        BlockEntityDataConverter converter = SchematicDataConversionManager.INSTANCE.getBlockEntityDataConverter(versionFrom.getMinecraftVersion(), versionTo);

        if (converter != null)
        {
            blockEntityList = converter.convertEntityNames(blockEntityList, idTagName);
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.error.schematic_conversion.missing_converter.block_entity_data",
                                        versionFrom.getMcVersionDisplayName(), String.valueOf(versionFrom.getDataVersion()), versionTo.getMcVersionDisplayName());
        }

        return blockEntityList;
    }

    public static NBTTagList convertBlockTickData(NBTTagList blockTickList, String idTagName, SchematicDataVersion versionFrom, MinecraftVersion versionTo)
    {
        BlockTickDataConverter converter = SchematicDataConversionManager.INSTANCE.getBlockTickDataConverter(versionFrom.getMinecraftVersion(), versionTo);

        if (converter != null)
        {
            blockTickList = converter.convertBlockNames(blockTickList, idTagName);
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.error.schematic_conversion.missing_converter.block_tick_data",
                                        versionFrom.getMcVersionDisplayName(), String.valueOf(versionFrom.getDataVersion()), versionTo.getMcVersionDisplayName());
        }

        return blockTickList;
    }

    public static NBTTagList convertEntityData(NBTTagList entityList, String idTagName, SchematicDataVersion versionFrom, MinecraftVersion versionTo)
    {
        EntityDataConverter converter = SchematicDataConversionManager.INSTANCE.getEntityDataConverter(versionFrom.getMinecraftVersion(), versionTo);

        if (converter != null)
        {
            entityList = converter.convertEntityNames(entityList, idTagName);
        }
        else
        {
            InfoUtils.printErrorMessage("litematica.error.schematic_conversion.missing_converter.entity_data",
                                        versionFrom.getMcVersionDisplayName(), String.valueOf(versionFrom.getDataVersion()), versionTo.getMcVersionDisplayName());
        }

        return entityList;
    }

    public static void convertEntitiesInList(NBTTagList entityList, String idTagName, @Nullable EntityDataConverterBase entityConverter, @Nullable InventoryDataConverter invConverter)
    {
        if (entityConverter != null)
        {
            entityConverter.convertEntityNames(entityList, idTagName);
        }

        if (invConverter != null)
        {
            invConverter.convertAnyInventoryContentsInList(entityList, idTagName);
        }
    }

    public static void convertEntityTag(NBTTagCompound tag, String idTagName, @Nullable EntityDataConverterBase entityConverter, @Nullable InventoryDataConverter invConverter)
    {
        if (entityConverter != null)
        {
            entityConverter.convertName(tag, idTagName);
        }

        if (invConverter != null)
        {
            // Get the converted name
            idTagName = tag.getString(idTagName);
            invConverter.convertAnyInventoryContents(idTagName, tag);
        }
    }

    public static IListTagDataConverter createBlockEntityListConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        boolean needsConversion = versionFrom != versionTo;
        final BlockEntityDataConverter beConverter = needsConversion ? SchematicDataConversionManager.INSTANCE.getBlockEntityDataConverter(versionFrom, versionTo) : null;
        final InventoryDataConverter invConverter = needsConversion ? SchematicDataConversionManager.INSTANCE.getInventoryDataConverter(versionFrom, versionTo) : null;

        return new EntityListDataConverter(beConverter, invConverter);
    }

    public static IListTagDataConverter createEntityListConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        boolean needsConversion = versionFrom != versionTo;
        final EntityDataConverter entityConverter = needsConversion ? SchematicDataConversionManager.INSTANCE.getEntityDataConverter(versionFrom, versionTo) : null;
        final InventoryDataConverter invConverter = needsConversion ? SchematicDataConversionManager.INSTANCE.getInventoryDataConverter(versionFrom, versionTo) : null;

        return new EntityListDataConverter(entityConverter, invConverter);
    }

    public static IListTagDataConverter createBlockTickListConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        boolean needsConversion = versionFrom != versionTo;
        final BlockTickDataConverter converter = needsConversion ? SchematicDataConversionManager.INSTANCE.getBlockTickDataConverter(versionFrom, versionTo) : null;

        return new BlockTickListDataConverter(converter);
    }

    public static class EntityListDataConverter implements IListTagDataConverter
    {
        @Nullable protected final EntityDataConverterBase entityConverter;
        @Nullable protected final InventoryDataConverter invConverter;

        public EntityListDataConverter(@Nullable EntityDataConverterBase entityConverter, @Nullable InventoryDataConverter invConverter)
        {
            this.entityConverter = entityConverter;
            this.invConverter = invConverter;
        }

        @Override
        public void convertData(NBTTagList tagList, String idTagName)
        {
            convertEntitiesInList(tagList, idTagName, this.entityConverter, this.invConverter);
        }

        @Nullable
        public EntityDataConverterBase getEntityDataConverter()
        {
            return this.entityConverter;
        }

        @Nullable
        public InventoryDataConverter getInventoryDataConverter()
        {
            return this.invConverter;
        }
    }

    public static class BlockTickListDataConverter implements IListTagDataConverter
    {
        @Nullable protected final BlockTickDataConverter converter;

        public BlockTickListDataConverter(@Nullable BlockTickDataConverter converter)
        {
            this.converter = converter;
        }

        @Override
        public void convertData(NBTTagList tagList, String idTagName)
        {
            if (this.converter != null)
            {
                this.converter.convertBlockNames(tagList, idTagName);
            }
        }
    }
}
