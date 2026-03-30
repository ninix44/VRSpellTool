package org.vmstudio.vrspelltool.forge;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.vrspelltool.core.client.SpellToolAddonClient;
import org.vmstudio.vrspelltool.core.common.VisorSpellTool;
import org.vmstudio.vrspelltool.core.server.SpellToolAddonServer;
import net.minecraftforge.fml.common.Mod;

@Mod(VisorSpellTool.MOD_ID)
public class SpellToolMod {
    public SpellToolMod() {
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
