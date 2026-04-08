package myau.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;

public class PacketUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final ThreadLocal<Integer> NO_EVENT_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static void sendPacket(Packet<?> packet) {
        mc.getNetHandler().getNetworkManager().sendPacket(packet);
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        NO_EVENT_DEPTH.set(NO_EVENT_DEPTH.get() + 1);
        try {
            mc.getNetHandler().getNetworkManager().sendPacket(packet, null);
        } finally {
            int depth = NO_EVENT_DEPTH.get() - 1;
            if (depth <= 0) {
                NO_EVENT_DEPTH.remove();
            } else {
                NO_EVENT_DEPTH.set(depth);
            }
        }
    }

    public static boolean shouldIgnoreEvents() {
        return NO_EVENT_DEPTH.get() > 0;
    }
}
