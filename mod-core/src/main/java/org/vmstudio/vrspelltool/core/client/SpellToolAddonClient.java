package org.vmstudio.vrspelltool.core.client;

import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.common.addon.VisorAddon;
import org.vmstudio.vrspelltool.core.client.voice.SpellDictionary;
import org.vmstudio.vrspelltool.core.common.VisorSpellTool;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpellToolAddonClient implements VisorAddon {
    @Override
    public void onAddonLoad() {
        SpellDictionary.initialize();
        VisorAPI.addonManager().getRegistries()
                .overlays()
                .registerComponents(java.util.List.of());
    }

    @Override
    public @Nullable String getAddonPackagePath() {
        return "org.vmstudio.vrspelltool.core.client";
    }

    @Override
    public @NotNull String getAddonId() {
        return VisorSpellTool.MOD_ID;
    }

    @Override
    public @NotNull Component getAddonName() {
        return Component.literal(VisorSpellTool.MOD_NAME);
    }

    @Override
    public String getModId() {
        return VisorSpellTool.MOD_ID;
    }
}
