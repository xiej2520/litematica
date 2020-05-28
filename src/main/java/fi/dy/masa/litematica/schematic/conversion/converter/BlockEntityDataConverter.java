package fi.dy.masa.litematica.schematic.conversion.converter;

import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;

public class BlockEntityDataConverter extends EntityDataConverterBase
{
    public BlockEntityDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        super(versionFrom, versionTo,
              "litematica.error.schematic_conversion.block_entity_name_mapping.failed_to_read_names_from_json_data",
              "litematica.message.info.schematic_conversion.block_entity_rename");
    }
}
