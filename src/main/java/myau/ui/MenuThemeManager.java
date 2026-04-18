package myau.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MenuThemeManager {
    private static final ResourceLocation[] LOCAL_BACKGROUNDS = new ResourceLocation[]{
            new ResourceLocation("myau", "menu/mainmenu_background1.png"),
            new ResourceLocation("myau", "menu/mainmenu_background2.png"),
            new ResourceLocation("myau", "menu/mainmenu_background3.png"),
            new ResourceLocation("myau", "menu/mainmenu_background4.png"),
            new ResourceLocation("myau", "menu/mainmenu_background5.png"),
            new ResourceLocation("myau", "menu/mainmenu_background6.png"),
            new ResourceLocation("myau", "menu/mainmenu_background7.png"),
            new ResourceLocation("myau", "menu/mainmenu_background8.png")
    };
    private static final String KEY_THEME_INDEX = "themeIndex";
    private static final String KEY_REMOTE_THEMES = "remoteThemes";
    private static final String REMOTE_THEME_NAMESPACE = "myau";
    private static final String REMOTE_THEME_PATH_PREFIX = "menu/remote_background_";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File THEME_FILE = new File(Minecraft.getMinecraft().mcDataDir, "openmyau.theme.json");
    private static final File REMOTE_CACHE_DIR = new File(Minecraft.getMinecraft().mcDataDir, "openmyau/theme-cache");
    private static final List<ResourceLocation> ACTIVE_BACKGROUNDS = new ArrayList<ResourceLocation>();
    private static final List<String> REMOTE_THEME_URLS = new ArrayList<String>();
    private static final Map<ResourceLocation, File> REMOTE_CACHE_FILES = new HashMap<ResourceLocation, File>();
    private static final Map<ResourceLocation, ImageSize> REMOTE_IMAGE_SIZES = new HashMap<ResourceLocation, ImageSize>();
    private static int selectedThemeIndex = 0;
    private static boolean loaded;

    private MenuThemeManager() {
    }

    public static synchronized int getThemeCount() {
        ensureLoaded();
        return ACTIVE_BACKGROUNDS.size();
    }

    public static synchronized int getSelectedThemeIndex() {
        ensureLoaded();
        return selectedThemeIndex;
    }

    public static synchronized ResourceLocation getCurrentBackground() {
        ensureLoaded();
        return ACTIVE_BACKGROUNDS.get(normalizeThemeIndex(selectedThemeIndex));
    }

    public static synchronized ResourceLocation getBackgroundByIndex(int index) {
        ensureLoaded();
        return ACTIVE_BACKGROUNDS.get(normalizeThemeIndex(index));
    }

    public static synchronized void setSelectedThemeIndex(int index) {
        ensureLoaded();
        selectedThemeIndex = normalizeThemeIndex(index);
        save();
    }

    public static synchronized int[] getCachedTextureSize(ResourceLocation texture) {
        ensureLoaded();

        ImageSize cachedSize = REMOTE_IMAGE_SIZES.get(texture);
        if (cachedSize != null) {
            return new int[]{cachedSize.width, cachedSize.height};
        }

        File cacheFile = REMOTE_CACHE_FILES.get(texture);
        if (cacheFile == null || !cacheFile.isFile()) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(cacheFile);
            if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
                ImageSize resolvedSize = new ImageSize(image.getWidth(), image.getHeight());
                REMOTE_IMAGE_SIZES.put(texture, resolvedSize);
                return new int[]{resolvedSize.width, resolvedSize.height};
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static synchronized boolean isRemoteTheme(ResourceLocation texture) {
        ensureLoaded();
        return REMOTE_CACHE_FILES.containsKey(texture);
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        loaded = true;
        load();
    }

    private static int normalizeThemeIndex(int index) {
        int count = ACTIVE_BACKGROUNDS.size();
        if (count <= 0) {
            return 0;
        }

        int normalized = index % count;
        return normalized < 0 ? normalized + count : normalized;
    }

    private static void load() {
        selectedThemeIndex = 0;
        REMOTE_THEME_URLS.clear();

        if (!THEME_FILE.exists()) {
            rebuildBackgrounds();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(THEME_FILE))) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (parsed != null && parsed.isJsonObject()) {
                JsonObject object = parsed.getAsJsonObject();
                if (object.has(KEY_THEME_INDEX) && object.get(KEY_THEME_INDEX).isJsonPrimitive()) {
                    selectedThemeIndex = object.get(KEY_THEME_INDEX).getAsInt();
                }

                if (object.has(KEY_REMOTE_THEMES) && object.get(KEY_REMOTE_THEMES).isJsonArray()) {
                    JsonArray remoteThemes = object.getAsJsonArray(KEY_REMOTE_THEMES);
                    for (JsonElement remoteTheme : remoteThemes) {
                        if (remoteTheme != null && remoteTheme.isJsonPrimitive()) {
                            String sanitized = sanitizeRemoteThemeUrl(remoteTheme.getAsString());
                            if (sanitized != null) {
                                REMOTE_THEME_URLS.add(sanitized);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        rebuildBackgrounds();
        selectedThemeIndex = normalizeThemeIndex(selectedThemeIndex);
    }

    private static void rebuildBackgrounds() {
        ACTIVE_BACKGROUNDS.clear();
        REMOTE_CACHE_FILES.clear();
        REMOTE_IMAGE_SIZES.clear();

        for (ResourceLocation localBackground : LOCAL_BACKGROUNDS) {
            if (isResourceAvailable(localBackground)) {
                ACTIVE_BACKGROUNDS.add(localBackground);
            }
        }

        if (REMOTE_THEME_URLS.isEmpty()) {
            if (ACTIVE_BACKGROUNDS.isEmpty()) {
                Collections.addAll(ACTIVE_BACKGROUNDS, LOCAL_BACKGROUNDS);
            }
            return;
        }

        File cacheParent = REMOTE_CACHE_DIR;
        if (!cacheParent.exists()) {
            cacheParent.mkdirs();
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        Set<String> uniqueUrls = new HashSet<String>();

        for (String remoteThemeUrl : REMOTE_THEME_URLS) {
            String sanitized = sanitizeRemoteThemeUrl(remoteThemeUrl);
            if (sanitized == null || !uniqueUrls.add(sanitized)) {
                continue;
            }

            String themeKey = Integer.toHexString(sanitized.hashCode());
            ResourceLocation remoteThemeResource = new ResourceLocation(
                    REMOTE_THEME_NAMESPACE,
                    REMOTE_THEME_PATH_PREFIX + themeKey
            );
            File cacheFile = new File(cacheParent, "theme_" + themeKey + ".img");

            ThreadDownloadImageData downloadedTexture = new ThreadDownloadImageData(
                    cacheFile,
                    sanitized,
                    LOCAL_BACKGROUNDS[0],
                    null
            );
            minecraft.getTextureManager().loadTexture(remoteThemeResource, downloadedTexture);

            ACTIVE_BACKGROUNDS.add(remoteThemeResource);
            REMOTE_CACHE_FILES.put(remoteThemeResource, cacheFile);
        }

        if (ACTIVE_BACKGROUNDS.isEmpty()) {
            Collections.addAll(ACTIVE_BACKGROUNDS, LOCAL_BACKGROUNDS);
        }
    }

    private static void save() {
        try {
            File parent = THEME_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            JsonObject object = new JsonObject();
            object.addProperty(KEY_THEME_INDEX, selectedThemeIndex);
            if (!REMOTE_THEME_URLS.isEmpty()) {
                JsonArray remoteThemes = new JsonArray();
                for (String remoteTheme : REMOTE_THEME_URLS) {
                    remoteThemes.add(new JsonPrimitive(remoteTheme));
                }
                object.add(KEY_REMOTE_THEMES, remoteThemes);
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(THEME_FILE))) {
                writer.write(GSON.toJson(object));
            }
        } catch (Throwable ignored) {
        }
    }

    private static String sanitizeRemoteThemeUrl(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return trimmed.startsWith("http://") || trimmed.startsWith("https://") ? trimmed : null;
    }

    private static boolean isResourceAvailable(ResourceLocation resourceLocation) {
        try (InputStream ignored = Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation).getInputStream()) {
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class ImageSize {
        private final int width;
        private final int height;

        private ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
