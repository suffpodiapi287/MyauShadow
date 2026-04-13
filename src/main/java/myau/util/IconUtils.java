package myau.util;

import org.lwjgl.opengl.Display;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class IconUtils {
    private static final int ALPHA_VISIBILITY_THRESHOLD = 16;
    private static final float ICON_FIT_SCALE = 1.12F;
    private static ByteBuffer[] iconBuffers;

    private IconUtils() {
    }

    public static void initLwjglIcon() {
        if (iconBuffers == null) {
            iconBuffers = loadIcons();
        }

        if (iconBuffers != null) {
            Display.setIcon(iconBuffers);
        }
    }

    private static ByteBuffer[] loadIcons() {
        try {
            ByteBuffer icon16 = readImageToBuffer("/assets/myau/icon_16x16.png");
            ByteBuffer icon32 = readImageToBuffer("/assets/myau/icon_32x32.png");
            ByteBuffer icon64 = readImageToBuffer("/assets/myau/icon_64x64.png");

            if (icon16 == null || icon32 == null || icon64 == null) {
                return null;
            }

            return new ByteBuffer[]{icon16, icon32, icon64};
        } catch (IOException ignored) {
            return null;
        }
    }

    private static ByteBuffer readImageToBuffer(String path) throws IOException {
        try (InputStream stream = IconUtils.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }

            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                return null;
            }

            BufferedImage fitted = fitVisibleArea(image, ICON_FIT_SCALE);
            int[] rgb = fitted.getRGB(0, 0, fitted.getWidth(), fitted.getHeight(), null, 0, fitted.getWidth());
            ByteBuffer buffer = ByteBuffer.allocate(4 * rgb.length);

            for (int color : rgb) {
                buffer.putInt(color << 8 | (color >> 24 & 255));
            }

            buffer.flip();
            return buffer;
        }
    }

    private static BufferedImage fitVisibleArea(BufferedImage source, float fitScale) {
        int width = source.getWidth();
        int height = source.getHeight();

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = source.getRGB(x, y) >>> 24;
                if (alpha <= ALPHA_VISIBILITY_THRESHOLD) {
                    continue;
                }

                if (x < minX) minX = x;
                if (y < minY) minY = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }

        if (maxX < minX || maxY < minY) {
            return source;
        }

        int visibleWidth = maxX - minX + 1;
        int visibleHeight = maxY - minY + 1;

        float safeFit = Math.min(1.25F, Math.max(0.8F, fitScale));
        float scale = Math.min((width * safeFit) / visibleWidth, (height * safeFit) / visibleHeight);
        int drawWidth = Math.max(1, Math.round(visibleWidth * scale));
        int drawHeight = Math.max(1, Math.round(visibleHeight * scale));
        int drawX = (width - drawWidth) / 2;
        int drawY = (height - drawHeight) / 2;

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(
                    source,
                    drawX, drawY, drawX + drawWidth, drawY + drawHeight,
                    minX, minY, maxX + 1, maxY + 1,
                    null
            );
        } finally {
            graphics.dispose();
        }
        return output;
    }
}
