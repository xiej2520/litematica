package fi.dy.masa.litematica.schematic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import fi.dy.masa.litematica.schematic.container.ILitematicaBlockStatePalette;
import fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainerFull;
import fi.dy.masa.litematica.schematic.conversion.MinecraftVersion;
import fi.dy.masa.malilib.gui.util.Message.MessageType;
import fi.dy.masa.malilib.util.BlockUtils;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.NBTUtils;

public class SpongeSchematic extends SingleRegionSchematic
{
    public static final String FILE_NAME_EXTENSION = ".schem";

    protected int version = 1;

    SpongeSchematic(File file)
    {
        super(file);
    }

    @Override
    public SchematicType<?> getType()
    {
        return SchematicType.SPONGE;
    }

    public static boolean isValidSchematic(NBTTagCompound tag)
    {
        if (tag.hasKey("Width", Constants.NBT.TAG_ANY_NUMERIC) &&
            tag.hasKey("Height", Constants.NBT.TAG_ANY_NUMERIC) &&
            tag.hasKey("Length", Constants.NBT.TAG_ANY_NUMERIC) &&
            tag.hasKey("Version", Constants.NBT.TAG_INT) &&
            tag.hasKey("Palette", Constants.NBT.TAG_COMPOUND) &&
            tag.hasKey("BlockData", Constants.NBT.TAG_BYTE_ARRAY))
        {
            return isSizeValid(readSizeFromTagImpl(tag));
        }

        return false;
    }

    @Override
    protected boolean initFromTag(NBTTagCompound tag)
    {
        this.version = tag.getInteger("Version");

        int dataVersion = MinecraftVersion.MC_1_13.getMaxDataVersion();

        if (this.version == 1)
        {
            dataVersion = MinecraftVersion.MC_1_12.getMaxDataVersion();;
        }
        else if (tag.hasKey("DataVersion", Constants.NBT.TAG_INT))
        {
            dataVersion = tag.getInteger("DataVersion");
        }

        this.setCurrentDataVersionWithFallback(dataVersion);

        return true;
    }

    @Override
    protected Vec3i readSizeFromTag(NBTTagCompound tag)
    {
        return readSizeFromTagImpl(tag);
    }

    private static Vec3i readSizeFromTagImpl(NBTTagCompound tag)
    {
        return new Vec3i(tag.getInteger("Width"), tag.getInteger("Height"), tag.getInteger("Length"));
    }

    @Override
    protected void readMetadataFromTag(NBTTagCompound tag)
    {
        super.readMetadataFromTag(tag);

        if (tag.hasKey("Metadata", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound metaTag = tag.getCompoundTag("Metadata");

            if (metaTag.hasKey("Date", Constants.NBT.TAG_LONG) &&
                this.getMetadata().getTimeCreated() <= 0)
            {
                long time = metaTag.getLong("Date");
                this.getMetadata().setTimeCreated(time);
                this.getMetadata().setTimeModified(time);
            }
        }
    }

    protected boolean readPaletteFromTag(NBTTagCompound tag, ILitematicaBlockStatePalette palette, boolean needsVersionConversion)
    {
        final int size = tag.getKeySet().size();

        if (needsVersionConversion)
        {
            NBTTagList paletteList = new NBTTagList();
            NBTTagCompound dummy = new NBTTagCompound();

            for (int i = 0; i < size; ++i)
            {
                paletteList.appendTag(dummy);
            }

            for (String key : tag.getKeySet())
            {
                int id = tag.getInteger(key);

                if (id < 0 || id >= size)
                {
                    InfoUtils.printErrorMessage("litematica.message.error.schematic_read.sponge.palette.invalid_id", id);
                    return false;
                }

                NBTTagCompound stateTag = BlockUtils.getBlockStateTagFromString(key);

                if (stateTag == null)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.error.schematic_read.sponge.palette.unknown_block", key);
                    stateTag = NBTUtil.writeBlockState(new NBTTagCompound(), LitematicaBlockStateContainerFull.AIR_BLOCK_STATE);
                }

                paletteList.set(id, stateTag);
            }

            paletteList = this.convertBlockStatePaletteToCurrentGameVersion(paletteList);

            return this.readPaletteFromLitematicaFormatTag(paletteList, palette);
        }
        else
        {
            List<IBlockState> list = new ArrayList<>(size);

            for (int i = 0; i < size; ++i)
            {
                list.add(null);
            }

            for (String key : tag.getKeySet())
            {
                int id = tag.getInteger(key);
                IBlockState state = BlockUtils.getBlockStateFromString(key);

                if (state == null)
                {
                    InfoUtils.showGuiOrInGameMessage(MessageType.WARNING, "litematica.message.error.schematic_read.sponge.palette.unknown_block", key);
                    state = LitematicaBlockStateContainerFull.AIR_BLOCK_STATE;
                }

                if (id < 0 || id >= size)
                {
                    InfoUtils.printErrorMessage("litematica.message.error.schematic_read.sponge.palette.invalid_id", id);
                    return false;
                }

                list.set(id, state);
            }

            return palette.setMapping(list);
        }
    }

    @Override
    protected boolean readBlocksFromTag(NBTTagCompound tag, boolean needsVersionConversion)
    {
        if (tag.hasKey("Palette", Constants.NBT.TAG_COMPOUND) &&
            tag.hasKey("BlockData", Constants.NBT.TAG_BYTE_ARRAY) &&
            isSizeValid(this.getSize()))
        {
            NBTTagCompound paletteTag = tag.getCompoundTag("Palette");
            byte[] blockData = tag.getByteArray("BlockData");
            int paletteSize = paletteTag.getKeySet().size();

            this.blockContainer = LitematicaBlockStateContainerFull.createContainer(paletteSize, blockData, this.getSize());

            if (this.blockContainer == null)
            {
                InfoUtils.printErrorMessage("litematica.message.error.schematic_read.sponge.failed_to_read_blocks");
                return false;
            }

            return this.readPaletteFromTag(paletteTag, this.blockContainer.getPalette(), needsVersionConversion);
        }

        return false;
    }

    @Override
    protected ImmutableMap<BlockPos, NBTTagCompound> readBlockEntitiesFromTag(NBTTagCompound tag, boolean needsVersionConversion)
    {
        ImmutableMap.Builder<BlockPos, NBTTagCompound> builder = ImmutableMap.builder();

        String tagName = this.version == 1 ? "TileEntities" : "BlockEntities";
        NBTTagList tagList = tag.getTagList(tagName, Constants.NBT.TAG_COMPOUND);

        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound beTag = tagList.getCompoundTagAt(i);
            BlockPos pos = NBTUtils.readBlockPosFromArrayTag(beTag, "Pos");

            if (pos != null && beTag.isEmpty() == false)
            {
                beTag.setString("id", beTag.getString("Id"));

                // Remove the Sponge tags from the data that is kept in memory
                beTag.removeTag("Id");
                beTag.removeTag("Pos");

                if (this.version == 1)
                {
                    beTag.removeTag("ContentVersion");
                }

                builder.put(pos, beTag);
            }
        }

        return builder.build();
    }

    @Override
    protected ImmutableList<EntityInfo> readEntitiesFromTag(NBTTagCompound tag, boolean needsVersionConversion)
    {
        ImmutableList.Builder<EntityInfo> builder = ImmutableList.builder();
        NBTTagList tagList = tag.getTagList("Entities", Constants.NBT.TAG_COMPOUND);
        final int size = tagList.tagCount();

        for (int i = 0; i < size; ++i)
        {
            NBTTagCompound entityData = tagList.getCompoundTagAt(i);
            Vec3d pos = NBTUtils.readVec3dFromListTag(entityData);

            if (pos != null && entityData.isEmpty() == false)
            {
                entityData.setString("id", entityData.getString("Id"));

                // Remove the Sponge tags from the data that is kept in memory
                entityData.removeTag("Id");

                builder.add(new EntityInfo(pos, entityData));
            }
        }

        return builder.build();
    }

    protected void writeMetadataToTag(NBTTagCompound tag)
    {
        NBTTagCompound metaTag = this.getMetadata().toTag();

        if (this.getMetadata().getTimeCreated() > 0)
        {
            metaTag.setLong("Date", this.getMetadata().getTimeCreated());
        }

        tag.setTag("Metadata", metaTag);
    }

    protected void writeBlocksToTag(NBTTagCompound tag)
    {
        NBTTagCompound paletteTag = this.writePaletteToTag(this.blockContainer.getPalette().getMapping());
        byte[] blockData = ((LitematicaBlockStateContainerFull) this.blockContainer).getBackingArrayAsByteArray();

        tag.setTag("Palette", paletteTag);
        tag.setByteArray("BlockData", blockData);
    }

    protected NBTTagCompound writePaletteToTag(List<IBlockState> list)
    {
        final int size = list.size();
        NBTTagCompound tag = new NBTTagCompound();

        for (int id = 0; id < size; ++id)
        {
            IBlockState state = list.get(id);
            tag.setInteger(state.toString(), id);
        }

        return tag;
    }

    protected void writeBlockEntitiesToTag(NBTTagCompound tag)
    {
        String tagName = this.version == 1 ? "TileEntities" : "BlockEntities";
        NBTTagList tagList = new NBTTagList();

        for (Map.Entry<BlockPos, NBTTagCompound> entry : this.blockEntities.entrySet())
        {
            NBTTagCompound beTag = entry.getValue().copy();
            NBTUtils.writeBlockPosToArrayTag(entry.getKey(), beTag, "Pos");

            // Add the Sponge tag and remove the vanilla/Litematica tag
            beTag.setString("Id", beTag.getString("id"));
            beTag.removeTag("id");

            if (this.version == 1)
            {
                beTag.setInteger("ContentVersion", 1);
            }

            tagList.appendTag(beTag);
        }

        tag.setTag(tagName, tagList);
    }

    protected void writeEntitiesToTag(NBTTagCompound tag)
    {
        NBTTagList tagList = new NBTTagList();

        for (EntityInfo info : this.entities)
        {
            NBTTagCompound entityData = info.nbt.copy();
            NBTUtils.writeVec3dToListTag(info.pos, entityData);

            // Add the Sponge tag and remove the vanilla/Litematica tag
            entityData.setString("Id", entityData.getString("id"));
            entityData.removeTag("id");

            if (this.version == 1)
            {
                entityData.setInteger("ContentVersion", 1);
            }

            tagList.appendTag(entityData);
        }

        tag.setTag("Entities", tagList);
    }

    @Override
    public NBTTagCompound toTag()
    {
        NBTTagCompound tag = new NBTTagCompound();

        this.writeBlocksToTag(tag);
        this.writeBlockEntitiesToTag(tag);
        this.writeEntitiesToTag(tag);
        this.writeMetadataToTag(tag);

        tag.setInteger("DataVersion", LitematicaSchematic.MINECRAFT_DATA_VERSION);
        tag.setInteger("Version", this.version);
        tag.setInteger("PaletteMax", this.blockContainer.getPalette().getPaletteSize() - 1);
        tag.setShort("Width", (short) this.getSize().getX());
        tag.setShort("Height", (short) this.getSize().getY());
        tag.setShort("Length", (short) this.getSize().getZ());

        return tag;
    }
}
