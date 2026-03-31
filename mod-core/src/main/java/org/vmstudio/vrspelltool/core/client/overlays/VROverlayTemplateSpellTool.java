package org.vmstudio.vrspelltool.core.client.overlays;

import me.phoenixra.atumvr.api.misc.color.AtumColor;
import org.vmstudio.visor.api.client.gui.overlays.RegisterVROverlayTemplate;
import org.vmstudio.visor.api.client.gui.overlays.framework.template.VROverlayTemplateScreen;
import org.vmstudio.visor.api.client.gui.overlays.options.OverlayOptionGroup;
import org.vmstudio.visor.api.client.gui.overlays.options.types.OverlayOptionsMisc;
import org.vmstudio.visor.api.client.gui.overlays.options.types.OverlayOptionsPose;
import org.vmstudio.visor.api.client.player.pose.PoseAnchor;
import org.vmstudio.visor.api.common.addon.VisorAddon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RegisterVROverlayTemplate(
        id = VROverlayTemplateSpellTool.ID,
        name = VROverlayTemplateSpellTool.NAME,
        description = VROverlayTemplateSpellTool.DESCRIPTION
)
public class VROverlayTemplateSpellTool extends VROverlayTemplateScreen {
    public static final String ID = "spelltool_template";
    public static final String NAME = "SpellTool Template";
    public static final String DESCRIPTION = "SpellTool Template Overlay";

    private final Component text = Component.literal("SpellTool Template Overlay");

    public VROverlayTemplateSpellTool(@NotNull VisorAddon owner, @NotNull String id) {
        super(owner, id);
        //if you want it to be enabled once created
        setEnabled(false); // true

    }

    @Override
    protected void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // guiGraphics.drawCenteredString(this.font, text,
         //       width / 2, height / 2, AtumColor.WHITE.asInt());

    }

    @Override
    protected boolean updateVisibility() {
        return false; // true
    }

    @Override
    protected @NotNull List<OverlayOptionGroup<?>> createTemplateOptions() {
        return List.of(
                new OverlayOptionsMisc(
                        this,
                        it -> {
                            it.setOptionsUpdaterType(OverlayOptionsMisc.OptionsUpdaterType.TICK);
                        }
                ),
                new OverlayOptionsPose(
                        this,
                        it -> {
                            it.setTickPose(true);
                            it.setAimedRotation(false);
                            it.setPositionAnchor(PoseAnchor.HMD);
                            it.setPositionOffsetX(0);
                            it.setPositionOffsetY(-0.1f);
                            it.setPositionOffsetZ(-1.2f);
                            it.setRotationAnchor(PoseAnchor.HMD);
                            it.setRotationOffsetX(0);
                            it.setRotationOffsetY(0);
                            it.setRotationOffsetZ(0);

                            it.setScale(1.0f);
                        }

                )
        );
    }
}
