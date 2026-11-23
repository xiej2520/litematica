package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.nbt.CompoundTag;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.WorldUtils;

@Mixin(value = SignBlockEntity.class, priority = 990)
public abstract class MixinSignBlockEntity
{
    // 1.15.2: the server's sign update can arrive after the edit screen opened,
    // replacing the inserted schematic text. Insert it again after the update.
    @Inject(method = "fromTag", at = @At("RETURN"))
    private void litematica_reinsertSignText(CompoundTag tag, CallbackInfo ci)
    {
        if (Configs.Generic.SIGN_TEXT_PASTE.getBooleanValue())
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc.currentScreen instanceof SignEditScreen &&
                ((IMixinSignEditScreen) mc.currentScreen).litematica_getSign() == (Object) this)
            {
                WorldUtils.insertSignTextFromSchematic((SignBlockEntity) (Object) this);
            }
        }
    }
}
