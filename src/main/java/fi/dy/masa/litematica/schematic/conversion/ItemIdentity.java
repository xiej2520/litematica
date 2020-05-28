package fi.dy.masa.litematica.schematic.conversion;

import java.util.Objects;
import javax.annotation.Nullable;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NBTTagCompound;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.NBTUtils;

public class ItemIdentity
{
    private String itemName;
    private int metadata;
    private boolean hasMetadata;
    private boolean isDamageable;
    private boolean damageInNbt;

    public ItemIdentity()
    {
    }

    public ItemIdentity(String itemName, int metadata, boolean hasMetadata, boolean damageable, boolean damageInNbt)
    {
        this.itemName = itemName;
        this.metadata = metadata;
        this.hasMetadata = hasMetadata;
        this.isDamageable = damageable;
        this.damageInNbt = damageInNbt;
    }

    public ItemIdentity setWithMeta(String itemName, int metadata, boolean damageable, boolean damageInNbt)
    {
        this.itemName = itemName;
        this.metadata = metadata;
        this.hasMetadata = true;
        this.isDamageable = damageable;
        this.damageInNbt = damageInNbt;
        return this;
    }

    public ItemIdentity setWithoutMeta(String itemName, boolean damageable, boolean damageInNbt)
    {
        this.itemName = itemName;
        this.metadata = 0;
        this.hasMetadata = false;
        this.isDamageable = damageable;
        this.damageInNbt = damageInNbt;
        return this;
    }

    public ItemIdentity setFromItem(NBTTagCompound tagIn)
    {
        this.itemName = tagIn.getString("id");
        this.hasMetadata = tagIn.hasKey("Damage", Constants.NBT.TAG_SHORT);

        if (this.hasMetadata)
        {
            this.metadata = tagIn.getShort("Damage");
        }

        return this;
    }

    public NBTTagCompound convertItem(NBTTagCompound tagIn, ItemIdentity other)
    {
        tagIn.setString("id", this.itemName);

        // Need to move the current damage value to or from the NBT tag
        if (this.isDamageable && this.damageInNbt != other.damageInNbt)
        {
            short damageInField = tagIn.getShort("Damage");

            // The item is damaged, transfer the old damage field into the NBT-based damage
            if (this.damageInNbt && damageInField != 0)
            {
                NBTTagCompound nbt = NBTUtils.getOrCreateCompound(tagIn, "tag");
                nbt.setInteger("Damage", damageInField);
            }
            else if (other.damageInNbt)
            {
                NBTTagCompound nbt = tagIn.getCompoundTag("tag");
                int damage = nbt.getInteger("Damage");

                if (damage != 0)
                {
                    tagIn.setShort("Damage", (short) damage);
                }

                if (nbt.getKeySet().size() == 1 && nbt.hasKey("Damage", Constants.NBT.TAG_INT))
                {
                    // Remove the NBT tag that only contained the Damage value
                    tagIn.removeTag("tag");
                }
            }
        }

        if (this.hasMetadata)
        {
            tagIn.setShort("Damage", (short) this.metadata);
        }
        else if (this.isDamageable == false)
        {
            tagIn.removeTag("Damage");
        }

        return tagIn;
    }

    @Nullable
    public static ItemIdentity fromJson(JsonObject obj)
    {
        if (JsonUtils.hasString(obj, "name"))
        {
            String itemName = JsonUtils.getString(obj, "name");
            boolean hasMeta = JsonUtils.hasInteger(obj, "meta");
            int meta = JsonUtils.getInteger(obj, "meta");
            boolean damageable = JsonUtils.getBoolean(obj, "damageable");
            boolean damageInNbt = JsonUtils.getBoolean(obj, "nbt_damage");

            return new ItemIdentity(itemName, meta, hasMeta, damageable, damageInNbt);
        }

        return null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ItemIdentity other = (ItemIdentity) o;

        return this.metadata == other.metadata &&
                this.hasMetadata == other.hasMetadata &&
                this.itemName.equals(other.itemName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.itemName, this.metadata, this.hasMetadata);
    }
}
