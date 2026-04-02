package org.vmstudio.vrspelltool.core.client.render;

import net.minecraft.world.phys.Vec3;

public final class LumosState {
    private static boolean active;
    private static Vec3 tip = Vec3.ZERO;

    private LumosState() {
    }

    public static boolean isActive() {
        return active;
    }

    public static Vec3 getTip() {
        return tip;
    }

    public static void setActive(boolean value) {
        active = value;
    }

    public static void setTip(Vec3 value) {
        tip = value;
    }

    public static void clear() {
        active = false;
        tip = Vec3.ZERO;
    }
}
