package fi.dy.masa.litematica.mixin;

import net.minecraft.client.render.model.json.ModelItemOverride;
import net.minecraft.item.ItemPropertyGetter;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

@Mixin(ModelItemOverride.class)
public abstract class MixinModelItemOverride
{
    @Redirect(method = "matches", at = @At(value = "INVOKE",
              target = "Lnet/minecraft/item/ItemPropertyGetter;call(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;)F"))
    private float fixCrashWithNullWorld(ItemPropertyGetter provider, ItemStack stack, World world, LivingEntity entity)
    {
        if (world == null)
        {
            return provider.call(stack, MinecraftClient.getInstance().world, entity);
        }

        return provider.call(stack, world, entity);
    }
}
