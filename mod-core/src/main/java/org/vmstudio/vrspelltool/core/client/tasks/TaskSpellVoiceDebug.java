package org.vmstudio.vrspelltool.core.client.tasks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayDeque;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.client.tasks.RegisterVisorTask;
import org.vmstudio.visor.api.client.tasks.TaskType;
import org.vmstudio.visor.api.client.tasks.VisorTask;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.common.addon.VisorAddon;
import org.vmstudio.visor.api.common.player.VRPose;
import org.vmstudio.vrspelltool.core.client.voice.SpellRecognitionService;
import org.vmstudio.vrspelltool.core.client.voice.SpellRecognitionSnapshot;

@RegisterVisorTask
public class TaskSpellVoiceDebug extends VisorTask {
    public static final String ID = "spell_voice_debug";

    private static final int STATUS_MESSAGE_INTERVAL_TICKS = 100;
    private static final int SPELL_PENDING_TIMEOUT_TICKS = 34;
    private static final int SPELL_ARM_TIMEOUT_TICKS = 60;
    private static final int GESTURE_MEMORY_TICKS = 20;
    private static final int SWING_SAMPLE_LIMIT = 4;
    private static final double MOTION_MIN_DISTANCE = 0.060D;
    private static final double MOTION_FORWARD = 0.040D;
    private static final double MOTION_UP = 0.045D;
    private static final double MOTION_SIDE = 0.045D;

    private final SpellRecognitionService recognitionService = new SpellRecognitionService();
    private int taskTicks;
    private int nextStatusMessageTick;
    private String lastStatusMessage = "";
    private String lastFinalTranscript = "";
    private String lastMatchedSpellId = "";
    private String lastMatchedSpellText = "";
    private int lastMatchedSpellTick;
    private @Nullable PendingSpell pendingSpell;
    private @Nullable SpellArmSession spellArmSession;
    private final ArrayDeque<HandSample> handSamples = new ArrayDeque<>();
    private final ArrayDeque<GestureEvent> recentGestureEvents = new ArrayDeque<>();

    public TaskSpellVoiceDebug(@NotNull VisorAddon owner) {
        super(owner);
    }

    @Override
    protected void onRun(@Nullable LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null) {
            player = minecraft.player;
        }
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        taskTicks++;

        recognitionService.ensureRunning(minecraft.gameDirectory.toPath());
        sampleHandPose();
        updateRecentGestures();

        SpellRecognitionSnapshot snapshot;
        while ((snapshot = recognitionService.poll()) != null) {
            handleSnapshot(player, snapshot);
        }

        tryCastPendingSpell(player);

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

        armGestureFromSpeech(snapshot.text());

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
        if (snapshot.partial() || snapshot.score() < 0.42D) {
            return;
        }

        if (snapshot.candidate().id().equals(lastMatchedSpellId)
                && snapshot.text().equals(lastMatchedSpellText)
                && taskTicks == lastMatchedSpellTick) {
            return;
        }

        pendingSpell = new PendingSpell(snapshot, taskTicks);
        armGestureForSpell(snapshot.candidate().id());
        if (hasMatchingGesture(snapshot.candidate().id())) {
            castSpell(player, snapshot);
        } else {
            sendChat(player, "[VOICE READY " + Math.round(snapshot.score() * 100.0D) + "%] \"" + snapshot.text() + "\" -> finish wand move");
        }
    }

    private void tryCastPendingSpell(LocalPlayer player) {
        if (pendingSpell == null) {
            return;
        }

        if (taskTicks - pendingSpell.heardTick() > SPELL_PENDING_TIMEOUT_TICKS) {
            pendingSpell = null;
            return;
        }

        if (hasMatchingGesture(pendingSpell.snapshot().candidate().id())) {
            castSpell(player, pendingSpell.snapshot());
        }
    }

    private void castSpell(LocalPlayer player, SpellRecognitionSnapshot snapshot) {
        spawnSpellParticles(player, snapshot);
        triggerSpellHaptics(snapshot);
        lastMatchedSpellId = snapshot.candidate().id();
        lastMatchedSpellText = snapshot.text();
        lastMatchedSpellTick = taskTicks;
        pendingSpell = null;
        spellArmSession = null;
        sendChat(player, "[CAST " + Math.round(snapshot.score() * 100.0D) + "%] \"" + snapshot.text() + "\" -> " + snapshot.candidate().displayName());
    }

    private void sampleHandPose() {
        VRPose handPose = VisorAPI.client().getVRLocalPlayer()
                .getPoseData(PlayerPoseType.TICK)
                .getMainHand();
        Vector3f position = new Vector3f(handPose.getPosition());
        handSamples.addFirst(new HandSample(position, taskTicks));
        while (handSamples.size() > SWING_SAMPLE_LIMIT) {
            handSamples.removeLast();
        }
    }

    private void armGestureFromSpeech(String text) {
        String spellId = detectSpellGestureStart(text);
        if (spellId == null) {
            return;
        }
        armGestureForSpell(spellId);
    }

    private @Nullable String detectSpellGestureStart(String text) {
        String normalized = text.toLowerCase(java.util.Locale.ROOT);
        if (containsAny(normalized, "avada", "ava da", "ava")) {
            return "avada_kedavra";
        }
        if (containsAny(normalized, "cru", "kru", "crew")) {
            return "crucio";
        }
        if (containsAny(normalized, "expel", "expelli", "expeli", "ekspel", "spell")) {
            return "expelliarmus";
        }
        if (containsAny(normalized, "lu", "lou", "lyu", "lumo", "lumo s")) {
            return "lumos";
        }
        if (containsAny(normalized, "sect", "sek", "sectum", "sektum")) {
            return "sectumsempra";
        }
        return null;
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private void armGestureForSpell(String spellId) {
        if (spellArmSession == null || !spellArmSession.spellId.equals(spellId)) {
            spellArmSession = new SpellArmSession(spellId, taskTicks);
        } else {
            spellArmSession.lastSpeechTick = taskTicks;
        }
    }

    private void updateRecentGestures() {
        while (!recentGestureEvents.isEmpty() && taskTicks - recentGestureEvents.peekFirst().tick() > GESTURE_MEMORY_TICKS) {
            recentGestureEvents.removeFirst();
        }
        if (spellArmSession != null && taskTicks - spellArmSession.lastSpeechTick > SPELL_ARM_TIMEOUT_TICKS) {
            spellArmSession = null;
        }
        HandMotion motion = computeHandMotion();
        if (motion == null) {
            return;
        }
        for (GestureType type : GestureType.values()) {
            if (motion.matches(type)) {
                if (recentGestureEvents.isEmpty()
                        || recentGestureEvents.peekLast().type() != type
                        || taskTicks - recentGestureEvents.peekLast().tick() > 1) {
                    recentGestureEvents.addLast(new GestureEvent(type, taskTicks));
                }
            }
        }
    }

    private @Nullable HandMotion computeHandMotion() {
        if (handSamples.size() < 3) {
            return null;
        }

        VRPose handPose = VisorAPI.client().getVRLocalPlayer()
                .getPoseData(PlayerPoseType.TICK)
                .getMainHand();
        Vector3f forward = new Vector3f(handPose.getDirection());
        if (forward.lengthSquared() <= 0.0001F) {
            return null;
        }
        forward.normalize();

        Vector3f worldUp = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f right = forward.cross(worldUp, new Vector3f());
        if (right.lengthSquared() <= 0.0001F) {
            right.set(1.0F, 0.0F, 0.0F);
        } else {
            right.normalize();
        }
        Vector3f up = right.cross(forward, new Vector3f()).normalize();

        HandSample newest = handSamples.getFirst();
        HandSample oldest = handSamples.getLast();
        Vector3f movement = newest.position().sub(oldest.position(), new Vector3f());
        double distance = movement.length();
        if (distance < MOTION_MIN_DISTANCE) {
            return null;
        }

        double forwardAmount = movement.dot(forward);
        double rightAmount = movement.dot(right);
        double upAmount = movement.dot(up);
        return new HandMotion(distance, forwardAmount, rightAmount, upAmount);
    }

    private boolean hasMatchingGesture(String spellId) {
        if (spellArmSession == null || !spellArmSession.spellId.equals(spellId)) {
            return false;
        }

        int earliestTick = spellArmSession.armTick - 2;
        int latestTick = spellArmSession.lastSpeechTick + SPELL_PENDING_TIMEOUT_TICKS;
        for (GestureEvent event : recentGestureEvents) {
            if (event.tick() < earliestTick) {
                continue;
            }
            if (event.tick() > latestTick) {
                continue;
            }
            if (matchesSpellGesture(spellId, event.type())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesSpellGesture(String spellId, GestureType gestureType) {
        return switch (spellId) {
            case "crucio" -> gestureType == GestureType.FORWARD
                    || gestureType == GestureType.SIDE
                    || gestureType == GestureType.UP;
            case "lumos" -> gestureType == GestureType.UP
                    || gestureType == GestureType.FORWARD
                    || gestureType == GestureType.SIDE;
            case "avada_kedavra" -> gestureType == GestureType.FORWARD
                    || gestureType == GestureType.SIDE
                    || gestureType == GestureType.UP;
            case "expelliarmus" -> gestureType == GestureType.FORWARD
                    || gestureType == GestureType.UP
                    || gestureType == GestureType.SIDE;
            case "sectumsempra" -> gestureType == GestureType.SIDE
                    || gestureType == GestureType.FORWARD
                    || gestureType == GestureType.UP;
            default -> gestureType == GestureType.FORWARD
                    || gestureType == GestureType.SIDE
                    || gestureType == GestureType.UP;
        };
    }

    private void sendChat(LocalPlayer player, String message) {
        player.displayClientMessage(Component.literal("[SpellTool] " + message), false);
    }

    private void spawnSpellParticles(LocalPlayer player, SpellRecognitionSnapshot snapshot) {
        Vector3f origin = getWandTipPosition();
        switch (snapshot.candidate().id()) {
            case "avada_kedavra" -> spawnAvadaParticles(player, origin);
            case "crucio" -> spawnCrucioParticles(player, origin);
            case "expelliarmus" -> spawnExpelliarmusParticles(player, origin);
            case "lumos" -> spawnLumosParticles(player, origin);
            case "sectumsempra" -> spawnSectumsempraParticles(player, origin);
            default -> spawnGenericParticles(player, origin);
        }
    }

    private Vector3f getWandTipPosition() {
        VRPose handPose = VisorAPI.client().getVRLocalPlayer()
                .getPoseData(PlayerPoseType.RENDER)
                .getMainHand();
        Vector3f origin = new Vector3f(handPose.getPosition());
        Vector3f direction = new Vector3f(handPose.getDirection());
        if (direction.lengthSquared() > 0.0001F) {
            origin.add(direction.normalize().mul(0.28F));
        }
        return origin;
    }

    private void spawnAvadaParticles(LocalPlayer player, Vector3fc origin) {
        spawnBurstRing(player, origin, ParticleTypes.SOUL_FIRE_FLAME, 16, 0.38D, 0.09D);
        spawnRandomCloud(player, origin, ParticleTypes.SMOKE, 10, 0.07D, 0.03D);
    }

    private void spawnCrucioParticles(LocalPlayer player, Vector3fc origin) {
        spawnBurstRing(player, origin, ParticleTypes.CRIT, 18, 0.34D, 0.12D);
        spawnRandomCloud(player, origin, ParticleTypes.DAMAGE_INDICATOR, 8, 0.05D, 0.02D);
    }

    private void spawnExpelliarmusParticles(LocalPlayer player, Vector3fc origin) {
        spawnBurstRing(player, origin, ParticleTypes.FIREWORK, 16, 0.40D, 0.14D);
        spawnRandomCloud(player, origin, ParticleTypes.ENCHANT, 12, 0.08D, 0.04D);
    }

    private void spawnLumosParticles(LocalPlayer player, Vector3fc origin) {
        spawnRandomCloud(player, origin, ParticleTypes.END_ROD, 16, 0.04D, 0.015D);
        spawnRandomCloud(player, origin, ParticleTypes.WAX_ON, 10, 0.03D, 0.010D);
        player.level().addParticle(
                new DustParticleOptions(new Vector3f(1.0F, 0.95F, 0.55F), 1.35F),
                origin.x(), origin.y(), origin.z(),
                0.0D, 0.02D, 0.0D
        );
    }

    private void spawnSectumsempraParticles(LocalPlayer player, Vector3fc origin) {
        for (int i = 0; i < 5; i++) {
            player.level().addParticle(
                    ParticleTypes.SWEEP_ATTACK,
                    origin.x() + 0.12D * i,
                    origin.y() + 0.03D * i,
                    origin.z(),
                    0.0D, 0.0D, 0.0D
            );
        }
        spawnRandomCloud(player, origin, ParticleTypes.ANGRY_VILLAGER, 6, 0.06D, 0.03D);
    }

    private void spawnGenericParticles(LocalPlayer player, Vector3fc origin) {
        spawnBurstRing(player, origin, ParticleTypes.ENCHANT, 12, 0.30D, 0.08D);
        spawnRandomCloud(player, origin, ParticleTypes.END_ROD, 6, 0.04D, 0.015D);
    }

    private void spawnBurstRing(LocalPlayer player,
                                Vector3fc origin,
                                ParticleOptions particle,
                                int count,
                                double radius,
                                double speed) {
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i) / count;
            player.level().addParticle(
                    particle,
                    origin.x() + Math.cos(angle) * radius,
                    origin.y() + (player.level().random.nextDouble() - 0.5D) * 0.08D,
                    origin.z() + Math.sin(angle) * radius,
                    Math.cos(angle) * speed,
                    0.01D + player.level().random.nextDouble() * 0.025D,
                    Math.sin(angle) * speed
            );
        }
    }

    private void spawnRandomCloud(LocalPlayer player,
                                  Vector3fc origin,
                                  ParticleOptions particle,
                                  int count,
                                  double spread,
                                  double speed) {
        for (int i = 0; i < count; i++) {
            player.level().addParticle(
                    particle,
                    origin.x() + (player.level().random.nextDouble() - 0.5D) * spread,
                    origin.y() + (player.level().random.nextDouble() - 0.5D) * spread,
                    origin.z() + (player.level().random.nextDouble() - 0.5D) * spread,
                    (player.level().random.nextDouble() - 0.5D) * speed,
                    player.level().random.nextDouble() * speed,
                    (player.level().random.nextDouble() - 0.5D) * speed
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
        pendingSpell = null;
        spellArmSession = null;
        handSamples.clear();
        recentGestureEvents.clear();
    }

    @Override
    public boolean isActive(@Nullable LocalPlayer player) {
        return Minecraft.getInstance().player != null && VisorAPI.clientState().stateMode().isActive();
    }

    @Override
    public @NotNull TaskType getType() {
        return TaskType.VR_PRE_RENDER;
    }

    @Override
    public @NotNull String getId() {
        return ID;
    }

    private record PendingSpell(SpellRecognitionSnapshot snapshot, int heardTick) {
    }

    private record HandSample(Vector3f position, int tick) {
    }

    private enum GestureType {
        FORWARD,
        UP,
        SIDE
    }

    private static final class SpellArmSession {
        private final String spellId;
        private final int armTick;
        private int lastSpeechTick;

        private SpellArmSession(String spellId, int tick) {
            this.spellId = spellId;
            this.armTick = tick;
            this.lastSpeechTick = tick;
        }
    }

    private record HandMotion(double distance, double forward, double side, double up) {
        private boolean matches(GestureType type) {
            return switch (type) {
                case FORWARD -> forward >= MOTION_FORWARD && distance >= MOTION_MIN_DISTANCE;
                case UP -> up >= MOTION_UP;
                case SIDE -> Math.abs(side) >= MOTION_SIDE;
            };
        }
    }

    private record GestureEvent(GestureType type, int tick) {
    }
}
