package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;

@Mixin(SignEditScreen.class)
public interface IMixinSignEditScreen
{
    @Accessor("sign")
    SignBlockEntity litematica_getSign();
}
