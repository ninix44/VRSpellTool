package org.vmstudio.vrspelltool.fabric;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.vrspelltool.core.client.SpellToolAddonClient;
import org.vmstudio.vrspelltool.core.server.SpellToolAddonServer;
import net.fabricmc.api.ModInitializer;

public class SpellToolMod implements ModInitializer {
    @Override
    public void onInitialize() {
        if (ModLoader.get().isDedicatedServer()) {
            VisorAPI.registerAddon(
                    new SpellToolAddonServer()
            );
        } else {
            VisorAPI.registerAddon(
                    new SpellToolAddonClient()
            );
        }
    }
}
