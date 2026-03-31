package org.vmstudio.vrspelltool.core.client.tasks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.tasks.RegisterVisorTask;
import org.vmstudio.visor.api.client.tasks.TaskType;
import org.vmstudio.visor.api.client.tasks.VisorTask;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.common.addon.VisorAddon;
//import org.vmstudio.vrspelltool.core.client.voice.SpellCandidate;
import org.vmstudio.vrspelltool.core.client.voice.SpellRecognitionService;
import org.vmstudio.vrspelltool.core.client.voice.SpellRecognitionSnapshot;

@RegisterVisorTask
public class TaskSpellVoiceDebug extends VisorTask {
    public static final String ID = "spell_voice_debug";

    private static final int STATUS_MESSAGE_INTERVAL_TICKS = 100;
    private static final int SPELL_REPEAT_COOLDOWN_TICKS = 32;

    private final SpellRecognitionService recognitionService = new SpellRecognitionService();
    private int taskTicks;
    private int nextStatusMessageTick;
    private String lastStatusMessage = "";
    private String lastFinalTranscript = "";
    private String lastMatchedSpellId = "";
    private String lastMatchedSpellText = "";
    private int lastMatchedSpellTick;

    public TaskSpellVoiceDebug(@NotNull VisorAddon owner) {
        super(owner);
    }

    @Override
    protected void onRun(@Nullable LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        taskTicks++;

        recognitionService.ensureRunning(minecraft.gameDirectory.toPath());

        SpellRecognitionSnapshot snapshot;
        while ((snapshot = recognitionService.poll()) != null) {
            handleSnapshot(player, snapshot);
        }

        String statusMessage = recognitionService.getStatusMessage();
        if (!statusMessage.equals(lastStatusMessage) && taskTicks >= nextStatusMessageTick) {
            sendChat(player, statusMessage);
            lastStatusMessage = statusMessage;
            nextStatusMessageTick = taskTicks + STATUS_MESSAGE_INTERVAL_TICKS;
        }
    }

    private void handleSnapshot(LocalPlayer player, SpellRecognitionSnapshot snapshot) {
        if (snapshot.text().isBlank()) {
            return;
        }

        if (snapshot.spellCast() && snapshot.candidate() != null) {
            handleSpellSnapshot(player, snapshot);
            return;
        }

        if (snapshot.partial()) {
            return;
        }

        if (snapshot.text().equals(lastFinalTranscript)) {
            return;
        }
        lastFinalTranscript = snapshot.text();

        sendChat(player, "[HEARD] \"" + snapshot.text() + "\"");
    }

    private void handleSpellSnapshot(LocalPlayer player, SpellRecognitionSnapshot snapshot) {
        if (snapshot.partial()) {
            return;
        }

        if (snapshot.score() < 0.42D) {
            return;
        }

        if (snapshot.candidate().id().equals(lastMatchedSpellId)
                && snapshot.text().equals(lastMatchedSpellText)
                && taskTicks - lastMatchedSpellTick < SPELL_REPEAT_COOLDOWN_TICKS) {
            return;
        }

        spawnSpellParticles(player);
        triggerSpellHaptics(snapshot);
        lastMatchedSpellId = snapshot.candidate().id();
        lastMatchedSpellText = snapshot.text();
        lastMatchedSpellTick = taskTicks;
        sendChat(player, "[MATCH " + Math.round(snapshot.score() * 100.0D) + "%] \"" + snapshot.text() + "\" -> " + snapshot.candidate().displayName());
    }

    private void sendChat(LocalPlayer player, String message) {
        player.displayClientMessage(Component.literal("[SpellTool] " + message), false);
    }

    private void spawnSpellParticles(LocalPlayer player) {
        for (int i = 0; i < 14; i++) {
            double angle = (Math.PI * 2.0D * i) / 14.0D;
            double radius = 0.55D;
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getEyeY() - 0.15D + (player.level().random.nextDouble() - 0.5D) * 0.2D;
            double z = player.getZ() + Math.sin(angle) * radius;
            player.level().addParticle(
                    ParticleTypes.ENCHANT,
                    x,
                    y,
                    z,
                    -Math.cos(angle) * 0.06D,
                    0.03D + player.level().random.nextDouble() * 0.03D,
                    -Math.sin(angle) * 0.06D
            );
        }

        for (int i = 0; i < 6; i++) {
            player.level().addParticle(
                    ParticleTypes.END_ROD,
                    player.getX() + (player.level().random.nextDouble() - 0.5D) * 0.6D,
                    player.getEyeY() - 0.05D + player.level().random.nextDouble() * 0.35D,
                    player.getZ() + (player.level().random.nextDouble() - 0.5D) * 0.6D,
                    (player.level().random.nextDouble() - 0.5D) * 0.02D,
                    0.02D + player.level().random.nextDouble() * 0.03D,
                    (player.level().random.nextDouble() - 0.5D) * 0.02D
            );
        }
    }

    private void triggerSpellHaptics(SpellRecognitionSnapshot snapshot) {
        float strength = snapshot.score() >= 0.85D ? 0.16F : 0.11F;
        if ("avada_kedavra".equals(snapshot.candidate().id())) {
            strength = 0.18F;
        } else if ("crucio".equals(snapshot.candidate().id())) {
            strength = 0.15F;
        }

        VisorAPI.client().getInputManager().triggerHapticPulse(HandType.MAIN, strength);
        VisorAPI.client().getInputManager().triggerHapticPulse(HandType.OFFHAND, strength * 0.9F);
    }

    @Override
    protected void onClear(@Nullable LocalPlayer player) {
        recognitionService.stop();
        taskTicks = 0;
        lastStatusMessage = "";
        lastFinalTranscript = "";
        nextStatusMessageTick = 0;
        lastMatchedSpellId = "";
        lastMatchedSpellText = "";
        lastMatchedSpellTick = 0;
    }

    @Override
    public boolean isActive(@Nullable LocalPlayer player) {
        return player != null && VisorAPI.clientState().stateMode().isActive();
    }

    @Override
    public @NotNull TaskType getType() {
        return TaskType.VR_PLAYER_TICK;
    }

    @Override
    public @NotNull String getId() {
        return ID;
    }
}
