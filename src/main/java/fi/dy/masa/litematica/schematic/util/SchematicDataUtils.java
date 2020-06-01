package fi.dy.masa.litematica.schematic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.litematica.schematic.EntityInfo;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStateContainer;
import fi.dy.masa.litematica.schematic.container.ILitematicaPalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerBase;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.schematic.conversion.ConversionUtils;
import fi.dy.masa.litematica.schematic.conversion.IListTagDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.BlockEntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.EntityDataConverter;
import fi.dy.masa.litematica.schematic.conversion.converter.InventoryDataConverter;
import fi.dy.masa.malilib.gui.util.Message;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;

public class SchematicDataUtils
{
    public static boolean readPaletteFromLitematicaFormatTag(NBTTagList convertedPaletteTag, NBTTagList originalPaletteTag, ILitematicaBlockStateContainer container)
    {
        final int size = convertedPaletteTag.tagCount();
        List<IBlockState> list = new ArrayList<>(size);
        ILitematicaPalette<NBTTagCompound> tagPalette = LitematicaBlockStateContainerBase.createPaletteWithSize(size);

        for (int id = 0; id < size; ++id)
        {
            NBTTagCompound tag = convertedPaletteTag.getCompoundTagAt(id);
            IBlockState state = NBTUtil.readBlockState(tag);
            list.add(state);

            NBTTagCompound tagOrig = originalPaletteTag.getCompoundTagAt(id);
            tagPalette.idFor(tagOrig);
        }

        container.setTagPalette(tagPalette);

        return container.getPalette().setMapping(list);
    }

    public static ImmutableMap<BlockPos, NBTTagCompound> readBlockEntitiesFromListTag(NBTTagList tagList, @Nullable BlockEntityDataConverter beConverter, @Nullable InventoryDataConverter invConverter)
    {
        ImmutableMap.Builder<BlockPos, NBTTagCompound> builder = ImmutableMap.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            BlockPos pos = NBTUtils.readBlockPos(tag);

            if (pos != null && tag.isEmpty() == false)
            {
                tag = tag.copy();
                NBTUtils.removeBlockPosFromTag(tag);
                ConversionUtils.convertEntityTag(tag, "id", beConverter, invConverter);
                builder.put(pos, tag);
            }
        }

        return builder.build();
    }

    public static ImmutableList<EntityInfo> readEntitiesFromListTag(NBTTagList tagList, @Nullable EntityDataConverter entityConverter, @Nullable InventoryDataConverter invConverter)
    {
        ImmutableList.Builder<EntityInfo> builder = ImmutableList.builder();
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            Vec3d posVec = NBTUtils.readVec3dFromListTag(tag);

            if (posVec != null && tag.isEmpty() == false)
            {
                tag = tag.copy();
                ConversionUtils.convertEntityTag(tag, "id", entityConverter, invConverter);
                builder.add(new EntityInfo(posVec, tag));
            }
        }

        return builder.build();
    }

    public static NBTTagList writePaletteToLitematicaFormatTag(ILitematicaBlockStateContainer container, boolean blocksUnmodified)
    {
        ILitematicaPalette<NBTTagCompound> tagPalette = container.getTagPalette();

        if (blocksUnmodified && tagPalette != null)
        {
            return SchematicDataUtils.writeTagPaletteToLitematicaFormatTag(tagPalette);
        }
        else
        {
            return SchematicDataUtils.writePaletteToLitematicaFormatTag(container.getPalette());
        }
    }

    public static NBTTagList writePaletteToLitematicaFormatTag(ILitematicaPalette<IBlockState> palette)
    {
        final int size = palette.getPaletteSize();
        List<IBlockState> list = palette.getMapping();
        NBTTagList tagList = new NBTTagList();

        for (int id = 0; id < size; ++id)
        {
            NBTTagCompound tag = new NBTTagCompound();
            NBTUtil.writeBlockState(tag, list.get(id));
            tagList.appendTag(tag);
        }

        return tagList;
    }

    public static NBTTagList writeTagPaletteToLitematicaFormatTag(ILitematicaPalette<NBTTagCompound> palette)
    {
        List<NBTTagCompound> mapping = palette.getMapping();
        NBTTagList tagList = new NBTTagList();

        for (NBTTagCompound tag : mapping)
        {
            tagList.appendTag(tag.copy());
        }

        return tagList;
    }

    public static NBTTagList writeEntitiesToListTag(List<EntityInfo> entityList)
    {
        NBTTagList tagList = new NBTTagList();

        if (entityList.isEmpty() == false)
        {
            for (EntityInfo info : entityList)
            {
                tagList.appendTag(info.nbt.copy());
            }
        }

        return tagList;
    }

    public static NBTTagList writeBlockEntitiesToListTag(Map<BlockPos, NBTTagCompound> tileMap)
    {
        NBTTagList tagList = new NBTTagList();

        if (tileMap.isEmpty() == false)
        {
            for (Map.Entry<BlockPos, NBTTagCompound> entry : tileMap.entrySet())
            {
                NBTTagCompound tag = entry.getValue().copy();
                NBTUtils.writeBlockPosToTag(entry.getKey(), tag);
                tagList.appendTag(tag);
            }
        }

        return tagList;
    }

    public static NBTTagCompound convertLitematicaPaletteToSpongePalette(NBTTagList list)
    {
        final int size = list.tagCount();
        NBTTagCompound tag = new NBTTagCompound();

        for (int id = 0; id < size; ++id)
        {
            String stateString = BlockUtils.getBlockStateStringFromTag(list.getCompoundTagAt(id));
            tag.setInteger(stateString, id);
        }

        return tag;
    }

    public static NBTTagList convertSpongePaletteTagToLitematicaPalette(NBTTagCompound tag)
    {
        NBTTagList paletteTag = new NBTTagList();
        NBTTagCompound dummy = new NBTTagCompound();
        final int size = tag.getKeySet().size();

        for (int i = 0; i < size; ++i)
        {
            paletteTag.appendTag(dummy);
        }

        for (String key : tag.getKeySet())
        {
            int id = tag.getInteger(key);

            if (id < 0 || id >= size)
            {
                InfoUtils.printErrorMessage("litematica.message.error.schematic_read.sponge.palette.invalid_id", id);
                continue;
            }

            NBTTagCompound stateTag = BlockUtils.getBlockStateTagFromString(key);

            if (stateTag == null)
            {
                InfoUtils.showGuiOrInGameMessage(Message.MessageType.WARNING, "litematica.message.error.schematic_read.sponge.palette.unknown_block", key);
                stateTag = NBTUtil.writeBlockState(new NBTTagCompound(), LitematicaBlockStateContainerFull.AIR_BLOCK_STATE);
            }

            paletteTag.set(id, stateTag);
        }

        return paletteTag;
    }

    public static void writeListDataToTag(
            BooleanSupplier canSaveFromCachedChecker,
            NBTTagCompound tag,
            @Nullable NBTTagCompound cachedTag,
            String tagName,
            String idTagName,
            @Nullable IListTagDataConverter dataConverter,
            Supplier<NBTTagList> listSupplier)
    {
        NBTTagList listTag;

        if (cachedTag != null && canSaveFromCachedChecker.getAsBoolean())
        {
            listTag = cachedTag.getTagList(tagName, Constants.NBT.TAG_COMPOUND).copy();
        }
        else
        {
            listTag = listSupplier.get();

            if (listTag == null)
            {
                listTag = new NBTTagList();
            }
        }

        if (dataConverter != null)
        {
            dataConverter.convertData(listTag, idTagName);
        }

        tag.setTag(tagName, listTag);
    }
}
