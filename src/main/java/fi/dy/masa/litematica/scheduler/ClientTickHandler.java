package fi.dy.masa.litematica.scheduler;

import net.minecraft.client.MinecraftClient;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.SelectionManager;
import fi.dy.masa.litematica.util.LayerUtils;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;

public class ClientTickHandler implements IClientTickHandler
{
    @Override
    public void onClientTick(MinecraftClient mc)
    {
        if (mc.world != null && mc.player != null)
        {
            SelectionManager sm = DataManager.getSelectionManager();

            if (sm.hasGrabbedElement())
            {
                sm.moveGrabbedElement(mc.player);
            }

            WorldUtils.easyPlaceOnUseTick(mc);

            LayerUtils.onClientTick(mc);

            DataManager.getSchematicPlacementManager().processQueuedChunks();
            TaskScheduler.getInstanceClient().runTasks();
        }
    }
}
