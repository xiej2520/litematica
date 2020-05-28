package fi.dy.masa.litematica.schematic.conversion;

public class EntityDataConverter extends EntityDataConverterBase
{
    protected EntityDataConverter(MinecraftVersion versionFrom, MinecraftVersion versionTo)
    {
        super(versionFrom, versionTo,
              "litematica.error.schematic_conversion.entity_name_mapping.failed_to_read_names_from_json_data",
              "litematica.message.info.schematic_conversion.entity_rename");
    }
}
