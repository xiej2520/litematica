package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.ContainerScreen;
import net.minecraft.text.Text;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;

@Mixin(ContainerScreen.class)
public abstract class MixinHandledScreen extends Screen
{
    private MixinHandledScreen(Text title)
    {
        super(title);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screen/ingame/ContainerScreen;drawBackground(FII)V"))
    private void renderSlotHighlights(int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        MaterialListHudRenderer.renderLookedAtBlockInInventory((ContainerScreen<?>) (Object) this, this.minecraft);
    }
}
