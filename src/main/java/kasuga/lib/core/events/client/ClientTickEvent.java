package kasuga.lib.core.events.client;

import kasuga.lib.KasugaLib;
import kasuga.lib.core.client.model.anim_instance.AnimateTickerManager;
import kasuga.lib.registrations.client.KeyBindingReg;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientTickEvent {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event){
        if(KasugaLib.STACKS.JAVASCRIPT.GROUP_CLIENT != null){
            KasugaLib.STACKS.JAVASCRIPT.GROUP_CLIENT.dispatchTick();
        }

        if(event.phase == TickEvent.Phase.END) {
            KeyBindingReg.onClientTick();
            AnimateTickerManager.INSTANCE.tickGui();
        }

        // deal with world ticker;
    }

    @SubscribeEvent
    public static void onGuiTick(TickEvent.LevelTickEvent event) {
        // deal with gui ticker;
        if (event.phase == TickEvent.Phase.END)
            AnimateTickerManager.INSTANCE.tickWorld();
    }
}
