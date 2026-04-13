package myau.util.font;

import myau.util.font.impl.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public final class FontManager {
    public static final String[] MANAGED_FONT_MODES = new String[]{
            "Minecraft",
            "Product Sans",
            "Regular",
            "Tenacity",
            "Vision",
            "NBP Informa",
            "Tahoma Bold",
            "Nunito Bold",
            "OpenSans Medium",
            "SanFrancisco",
            "Roboto Medium",
            "Minecraft Font",
            "Stratum2 Medium",
            "CS",
            "Mojang"
    };

    private static final Map<String, FontRenderer> LEGACY_FONT_CACHE = new HashMap<>();
    private static final Map<String, TTFFontRenderer> FONT_CACHE = new HashMap<>();
    private static int legacyScaleFactor = -1;
    private static final ManagedFont MINECRAFT_FONT = new ManagedFont() {
        @Override
        public void drawString(String text, float x, float y, int color) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null && minecraft.fontRendererObj != null) {
                minecraft.fontRendererObj.drawString(text, x, y, color, false);
            }
        }

        @Override
        public void drawStringWithShadow(String text, float x, float y, int color) {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null && minecraft.fontRendererObj != null) {
                minecraft.fontRendererObj.drawStringWithShadow(text, x, y, color);
            }
        }

        @Override
        public float getStringWidth(String text) {
            Minecraft minecraft = Minecraft.getMinecraft();
            return minecraft != null && minecraft.fontRendererObj != null ? minecraft.fontRendererObj.getStringWidth(text) : 0.0F;
        }

        @Override
        public float getHeight() {
            Minecraft minecraft = Minecraft.getMinecraft();
            return minecraft != null && minecraft.fontRendererObj != null ? minecraft.fontRendererObj.FONT_HEIGHT : 0.0F;
        }
    };

    public static FontRenderer regular22;
    public static FontRenderer productSans20;
    public static FontRenderer tenacity20;
    public static FontRenderer vision20;
    public static FontRenderer nbpInforma20;
    public static FontRenderer tahomaBold20;
    public static FontRenderer nunitoBold48;

    private FontManager() {
    }

    public static int indexOfManagedFont(String mode) {
        if (mode == null) {
            return 0;
        }
        for (int i = 0; i < MANAGED_FONT_MODES.length; i++) {
            if (MANAGED_FONT_MODES[i].equalsIgnoreCase(mode)) {
                return i;
            }
        }
        return 0;
    }

    public static ManagedFont getManagedFont(String mode, int size) {
        if (mode == null || mode.trim().isEmpty()) {
            return MINECRAFT_FONT;
        }

        String normalized = mode.toUpperCase(Locale.ROOT);
        if ("MINECRAFT".equals(normalized)) {
            return MINECRAFT_FONT;
        }

        FontRenderer legacyFont = getLegacyFont(mode, Math.max(8, size));
        if (legacyFont != null) {
            return wrapLegacyFont(legacyFont);
        }

        TTFFontRenderer dogSenseFont = getDogSenseFont("MINECRAFT FONT".equals(normalized) ? "MINECRAFT" : mode, Math.max(8, size));
        if (dogSenseFont != null) {
            return wrapDogSenseFont(dogSenseFont);
        }

        return MINECRAFT_FONT;
    }

    public static void ensureLegacyFonts() {
        int currentScaleFactor = getScaleFactor();
        if (currentScaleFactor == legacyScaleFactor && !LEGACY_FONT_CACHE.isEmpty()) {
            return;
        }

        releaseLegacyFonts();
        legacyScaleFactor = currentScaleFactor;

        regular22 = getOrCreateLegacy("REGULAR:22", () -> loadLegacyFont(
                new String[]{"/assets/myau/fonts/regular.ttf", "/assets/myau/fonts/OpenSans-Variable.ttf", "/assets/myau/fonts/product_sans_regular.ttf"},
                new String[]{"Open Sans", "SansSerif"},
                22.0F
        ));
        productSans20 = getOrCreateLegacy("PRODUCT_SANS:20", () -> loadLegacyFont(
                new String[]{"/assets/myau/fonts/product_sans_regular.ttf", "/assets/myau/fonts/OpenSans-Variable.ttf"},
                new String[]{"Product Sans", "Open Sans", "SansSerif"},
                20.0F
        ));
        tenacity20 = getOrCreateLegacy("TENACITY:20", () -> loadLegacyFont(
                new String[]{"/assets/myau/fonts/tenacity.ttf", "/assets/myau/fonts/OpenSans-Variable.ttf", "/assets/myau/fonts/product_sans_regular.ttf"},
                new String[]{"Open Sans", "SansSerif"},
                20.0F
        ));
        vision20 = getOrCreateLegacy("VISION:20", () -> loadLegacyFont(
                new String[]{"/assets/myau/fonts/Vision.otf", "/assets/myau/fonts/SF-Pro.ttf", "/assets/myau/fonts/Roboto-Medium.ttf"},
                new String[]{"SF Pro Display", "Roboto", "SansSerif"},
                20.0F
        ));
        nbpInforma20 = getOrCreateLegacy("NBP_INFORMA:20", () -> loadLegacyFont(
                new String[]{"/assets/myau/fonts/nbp-informa-fivesix.ttf", "/assets/myau/fonts/MinecraftRegular.otf", "/assets/myau/fonts/mojangles.ttf"},
                new String[]{"Minecraft", "Monospaced"},
                20.0F
        ));
        tahomaBold20 = getOrCreateLegacy("TAHOMA_BOLD:20", () -> loadWeightedLegacyFont(
                new String[]{"/assets/myau/fonts/tahomabold.ttf", "/assets/myau/fonts/Tahoma.ttf"},
                new String[]{"Tahoma", "SansSerif"},
                20.0F,
                TextAttribute.WEIGHT_BOLD
        ));
        nunitoBold48 = getOrCreateLegacy("NUNITO_BOLD:48", () -> loadWeightedLegacyFont(
                new String[]{"/assets/myau/fonts/Nunito-Bold.ttf", "/assets/myau/fonts/Roboto-Medium.ttf", "/assets/myau/fonts/OpenSans-Variable.ttf"},
                new String[]{"Nunito", "Roboto", "Open Sans", "SansSerif"},
                48.0F,
                TextAttribute.WEIGHT_BOLD
        ));
    }

    public static FontRenderer getLegacyHudFont(String mode) {
        ensureLegacyFonts();
        return getLegacyFont(mode, 20);
    }

    public static TTFFontRenderer getDogSenseFont(String mode, int size) {
        if (mode == null) {
            return null;
        }
        int resolvedSize = Math.max(8, size);
        switch (mode.toUpperCase(Locale.ROOT)) {
            case "OPENSANS MEDIUM":
                return getOrCreateDogSenseFont("OPENSANS MEDIUM:" + resolvedSize, () -> loadWeightedFont(
                        new String[]{"/assets/myau/fonts/OpenSans-Medium.ttf", "/assets/myau/fonts/OpenSans-Variable.ttf", "/assets/myau/fonts/product_sans_medium.ttf"},
                        new String[]{"Open Sans", "SansSerif"},
                        resolvedSize,
                        TextAttribute.WEIGHT_MEDIUM
                ));
            case "SANFRANCISCO":
                return getOrCreateDogSenseFont("SANFRANCISCO:" + resolvedSize, () -> loadFont(
                        new String[]{"/assets/myau/fonts/SF-Pro.ttf", "/assets/myau/fonts/sfui_medium.ttf"},
                        new String[]{"SF Pro Display", "SF Pro Text", "SansSerif"},
                        resolvedSize
                ));
            case "ROBOTO MEDIUM":
                return getOrCreateDogSenseFont("ROBOTO MEDIUM:" + resolvedSize, () -> loadWeightedFont(
                        new String[]{"/assets/myau/fonts/Roboto-Medium.ttf", "/assets/myau/fonts/Roboto-Variable.ttf"},
                        new String[]{"Roboto", "SansSerif"},
                        resolvedSize,
                        TextAttribute.WEIGHT_MEDIUM
                ));
            case "MINECRAFT":
                return getOrCreateDogSenseFont("MINECRAFT:" + resolvedSize, () -> loadFont(
                        new String[]{"/assets/myau/fonts/MinecraftRegular.otf", "/assets/myau/fonts/MinecraftRegular.ttf", "/assets/myau/fonts/mojangles.ttf"},
                        new String[]{"Minecraft", "Monospaced"},
                        resolvedSize
                ));
            case "STRATUM2 MEDIUM":
                return getOrCreateDogSenseFont("STRATUM2 MEDIUM:" + resolvedSize, () -> loadFont(
                        new String[]{"/assets/myau/fonts/Stratum2-Medium.ttf", "/assets/myau/fonts/Tahoma.ttf"},
                        new String[]{"Stratum2", "Tahoma", "SansSerif"},
                        resolvedSize
                ));
            case "CS":
                return getOrCreateDogSenseFont("CS:" + resolvedSize, () -> loadFont(
                        new String[]{"/assets/myau/fonts/esp-icons.ttf", "/assets/myau/fonts/stylesicons.ttf"},
                        new String[]{"Dialog"},
                        resolvedSize
                ));
            case "MOJANG":
                return getOrCreateDogSenseFont("MOJANG:" + resolvedSize, () -> loadFont(
                        new String[]{"/assets/myau/fonts/Mojang-Regular.ttf", "/assets/myau/fonts/mojanglesbold.ttf", "/assets/myau/fonts/mojangles.ttf"},
                        new String[]{"Minecraft", "Monospaced"},
                        resolvedSize
                ));
            default:
                return null;
        }
    }

    private static FontRenderer getOrCreateLegacy(String key, Supplier<Font> fontSupplier) {
        FontRenderer cached = LEGACY_FONT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        FontRenderer renderer = new FontRenderer(fontSupplier.get());
        LEGACY_FONT_CACHE.put(key, renderer);
        return renderer;
    }

    private static TTFFontRenderer getOrCreateDogSenseFont(String key, Supplier<Font> fontSupplier) {
        TTFFontRenderer cached = FONT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        TTFFontRenderer renderer = new TTFFontRenderer(fontSupplier.get());
        FONT_CACHE.put(key, renderer);
        return renderer;
    }

    private static FontRenderer getLegacyFont(String mode, int size) {
        if (mode == null) {
            return null;
        }

        int resolvedSize = Math.max(8, size);
        switch (mode.toUpperCase(Locale.ROOT)) {
            case "PRODUCT SANS":
                return getOrCreateLegacy("PRODUCT_SANS:" + resolvedSize, () -> loadLegacyFont(
                        new String[]{"/assets/myau/fonts/product_sans_regular.ttf", "/assets/myau/fonts/OpenSans-Variable.ttf"},
                        new String[]{"Product Sans", "Open Sans", "SansSerif"},
                        resolvedSize
                ));
            case "REGULAR":
                return getOrCreateLegacy("REGULAR:" + resolvedSize, () -> loadLegacyFont(
                        new String[]{"/assets/myau/fonts/regular.ttf", "/assets/myau/fonts/OpenSans-Variable.ttf", "/assets/myau/fonts/product_sans_regular.ttf"},
                        new String[]{"Open Sans", "SansSerif"},
                        resolvedSize
                ));
            case "TENACITY":
                return getOrCreateLegacy("TENACITY:" + resolvedSize, () -> loadLegacyFont(
                        new String[]{"/assets/myau/fonts/tenacity.ttf", "/assets/myau/fonts/OpenSans-Variable.ttf", "/assets/myau/fonts/product_sans_regular.ttf"},
                        new String[]{"Open Sans", "SansSerif"},
                        resolvedSize
                ));
            case "VISION":
                return getOrCreateLegacy("VISION:" + resolvedSize, () -> loadLegacyFont(
                        new String[]{"/assets/myau/fonts/Vision.otf", "/assets/myau/fonts/SF-Pro.ttf", "/assets/myau/fonts/Roboto-Medium.ttf"},
                        new String[]{"SF Pro Display", "Roboto", "SansSerif"},
                        resolvedSize
                ));
            case "NBP INFORMA":
                return getOrCreateLegacy("NBP_INFORMA:" + resolvedSize, () -> loadLegacyFont(
                        new String[]{"/assets/myau/fonts/nbp-informa-fivesix.ttf", "/assets/myau/fonts/MinecraftRegular.otf", "/assets/myau/fonts/mojangles.ttf"},
                        new String[]{"Minecraft", "Monospaced"},
                        resolvedSize
                ));
            case "TAHOMA BOLD":
                return getOrCreateLegacy("TAHOMA_BOLD:" + resolvedSize, () -> loadWeightedLegacyFont(
                        new String[]{"/assets/myau/fonts/tahomabold.ttf", "/assets/myau/fonts/Tahoma.ttf"},
                        new String[]{"Tahoma", "SansSerif"},
                        resolvedSize,
                        TextAttribute.WEIGHT_BOLD
                ));
            case "NUNITO BOLD":
                return getOrCreateLegacy("NUNITO_BOLD:" + resolvedSize, () -> loadWeightedLegacyFont(
                        new String[]{"/assets/myau/fonts/Nunito-Bold.ttf", "/assets/myau/fonts/Roboto-Medium.ttf", "/assets/myau/fonts/OpenSans-Variable.ttf"},
                        new String[]{"Nunito", "Roboto", "Open Sans", "SansSerif"},
                        resolvedSize,
                        TextAttribute.WEIGHT_BOLD
                ));
            default:
                return null;
        }
    }

    private static void releaseLegacyFonts() {
        for (FontRenderer renderer : LEGACY_FONT_CACHE.values()) {
            if (renderer != null) {
                renderer.destroy();
            }
        }
        LEGACY_FONT_CACHE.clear();
        regular22 = null;
        productSans20 = null;
        tenacity20 = null;
        vision20 = null;
        nbpInforma20 = null;
        tahomaBold20 = null;
        nunitoBold48 = null;
    }

    private static Font loadLegacyFont(String[] resourcePaths, String[] fallbackFamilies, float size) {
        return loadFont(resourcePaths, fallbackFamilies, resolveLegacySize(size));
    }

    private static ManagedFont wrapLegacyFont(FontRenderer renderer) {
        return new ManagedFont() {
            @Override
            public void drawString(String text, float x, float y, int color) {
                renderer.drawString(text, x, y, color);
            }

            @Override
            public void drawStringWithShadow(String text, float x, float y, int color) {
                renderer.drawStringWithShadow(text, x, y, color);
            }

            @Override
            public float getStringWidth(String text) {
                return (float) renderer.getStringWidth(text);
            }

            @Override
            public float getHeight() {
                return (float) renderer.getHeight();
            }
        };
    }

    private static ManagedFont wrapDogSenseFont(TTFFontRenderer renderer) {
        return new ManagedFont() {
            @Override
            public void drawString(String text, float x, float y, int color) {
                renderer.drawString(text, x, y, color);
            }

            @Override
            public void drawStringWithShadow(String text, float x, float y, int color) {
                renderer.drawStringWithShadow(text, x, y, color);
            }

            @Override
            public float getStringWidth(String text) {
                return renderer.getWidth(text);
            }

            @Override
            public float getHeight() {
                return renderer.getHeight("Ag");
            }
        };
    }

    private static Font loadWeightedLegacyFont(String[] resourcePaths, String[] fallbackFamilies, float size, Float weight) {
        return loadWeightedFont(resourcePaths, fallbackFamilies, resolveLegacySize(size), weight);
    }

    private static Font loadFont(String[] resourcePaths, String[] fallbackFamilies, float size) {
        for (String resourcePath : resourcePaths) {
            Font resourceFont = loadResourceFont(resourcePath, size);
            if (resourceFont != null) {
                return resourceFont;
            }
        }
        for (String family : fallbackFamilies) {
            if (familyExists(family)) {
                return new Font(family, Font.PLAIN, Math.round(size)).deriveFont(size);
            }
        }
        return new Font("Dialog", Font.PLAIN, Math.round(size)).deriveFont(size);
    }

    private static float resolveLegacySize(float size) {
        return size * Math.max(1, getScaleFactor()) / 2.0F;
    }

    private static int getScaleFactor() {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null) {
                return new ScaledResolution(minecraft).getScaleFactor();
            }
        } catch (Throwable ignored) {
        }
        return 2;
    }

    private static Font loadWeightedFont(String[] resourcePaths, String[] fallbackFamilies, float size, Float weight) {
        Font font = loadFont(resourcePaths, fallbackFamilies, size);
        Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.SIZE, size);
        attributes.put(TextAttribute.WEIGHT, weight);
        return font.deriveFont(attributes);
    }

    private static Font loadResourceFont(String resourcePath, float size) {
        try (InputStream inputStream = FontManager.class.getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                return Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont(size);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean familyExists(String family) {
        try {
            String[] familyNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ROOT);
            for (String familyName : familyNames) {
                if (familyName.equalsIgnoreCase(family)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
