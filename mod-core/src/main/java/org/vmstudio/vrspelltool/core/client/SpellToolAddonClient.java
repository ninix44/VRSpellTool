package org.vmstudio.vrspelltool.core.client;

import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.common.addon.VisorAddon;
import org.vmstudio.vrspelltool.core.client.overlays.VROverlaySpellTool;
import org.vmstudio.vrspelltool.core.client.overlays.VROverlayTemplateSpellTool;
import org.vmstudio.vrspelltool.core.common.VisorSpellTool;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpellToolAddonClient implements VisorAddon {
    @Override
    public void onAddonLoad() {
        VisorAPI.addonManager().getRegistries()
                .overlays()
                .registerComponents(
                        List.of(
                                new VROverlaySpellTool(
                                        this,
                                        VROverlaySpellTool.ID
                                ),
                                new VROverlayTemplateSpellTool(
                                        this,
                                        VROverlayTemplateSpellTool.ID
                                )
                        )
                );
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
