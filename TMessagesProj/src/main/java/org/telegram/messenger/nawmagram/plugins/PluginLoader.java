package org.telegram.messenger.nawmagram.plugins;

import android.content.Context;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads Nawmagram plugins:
 * 1. Plaintext JavaScript (.naw, .js)
 * 2. Zip-packaged JS plugins (with plugin.js)
 * 3. Compiled DEX/APK plugins (.dex, .apk)
 */
public class PluginLoader {

    private static final String[] DEFAULT_CLASS_NAMES = {
            "PluginMain",
            "MainPlugin",
            "org.nawmagram.plugin.PluginMain",
            "com.nawmagram.plugin.PluginMain"
    };

    public static LoadedPlugin loadPlugin(Context context, File file, boolean enabled) {
        if (file == null || !file.exists() || !file.canRead()) {
            return null;
        }

        // 1. Try loading as a plaintext JS script (.naw or .js)
        if (isTextScript(file)) {
            return loadJsTextPlugin(context, file, enabled);
        }

        // 2. Try loading as ZIP package (can be JS zip package or DEX zip package)
        if (isZipFile(file)) {
            LoadedPlugin zipJs = tryLoadZipJsPlugin(context, file, enabled);
            if (zipJs != null) {
                return zipJs;
            }
        }

        // 3. Fallback: DexClassLoader for compiled .dex / .apk
        return loadDexPlugin(context, file, enabled);
    }

    private static boolean isZipFile(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[4];
            if (fis.read(header) == 4) {
                return header[0] == 0x50 && header[1] == 0x4B && header[2] == 0x03 && header[3] == 0x04;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean isTextScript(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".js")) {
            return true;
        }
        if (!isZipFile(file)) {
            // Check if first bytes are readable text
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buf = new byte[256];
                int n = fis.read(buf);
                if (n > 0) {
                    for (int i = 0; i < n; i++) {
                        byte b = buf[i];
                        if (b == 0) return false; // binary
                    }
                    return true;
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    private static LoadedPlugin loadJsTextPlugin(Context context, File file, boolean enabled) {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String code = sb.toString();
            PluginMeta meta = parseHeaderMetadata(code, file.getName());

            JsNawmaPlugin instance = new JsNawmaPlugin(
                    meta.id,
                    meta.name,
                    meta.version,
                    meta.author,
                    meta.description,
                    code
            );

            File pluginDir = new File(context.getFilesDir(), "nawma_plugin_data/" + meta.id);
            PluginContext pluginContext = new PluginContext(context, meta.id, pluginDir);
            instance.onLoad(pluginContext);

            LoadedPlugin loaded = new LoadedPlugin(meta.id, file, instance, PluginLoader.class.getClassLoader(), enabled);
            if (enabled) {
                try {
                    instance.onEnable();
                } catch (Throwable t) {
                    FileLog.e(t);
                    loaded.setEnabled(false);
                    loaded.setLoadError("Ошибка запуска: " + t.getMessage());
                }
            }
            return loaded;
        } catch (Throwable t) {
            FileLog.e("[PluginLoader] Failed to read JS plugin file: " + file.getName(), t);
            LoadedPlugin failed = new LoadedPlugin(file.getName(), file, null, null, false);
            failed.setLoadError("Ошибка чтения: " + t.getMessage());
            return failed;
        }
    }

    private static LoadedPlugin tryLoadZipJsPlugin(Context context, File file, boolean enabled) {
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry jsEntry = zip.getEntry("plugin.js");
            if (jsEntry == null) jsEntry = zip.getEntry("script.js");
            if (jsEntry == null) jsEntry = zip.getEntry("main.js");

            if (jsEntry != null) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(jsEntry), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                String code = sb.toString();
                PluginMeta meta = readMetadata(file);
                if (meta.name == null) {
                    meta = parseHeaderMetadata(code, file.getName());
                }

                JsNawmaPlugin instance = new JsNawmaPlugin(
                        meta.id,
                        meta.name,
                        meta.version,
                        meta.author,
                        meta.description,
                        code
                );

                File pluginDir = new File(context.getFilesDir(), "nawma_plugin_data/" + meta.id);
                PluginContext pluginContext = new PluginContext(context, meta.id, pluginDir);
                instance.onLoad(pluginContext);

                LoadedPlugin loaded = new LoadedPlugin(meta.id, file, instance, PluginLoader.class.getClassLoader(), enabled);
                if (enabled) {
                    try {
                        instance.onEnable();
                    } catch (Throwable t) {
                        FileLog.e(t);
                        loaded.setEnabled(false);
                        loaded.setLoadError("Ошибка запуска: " + t.getMessage());
                    }
                }
                return loaded;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static PluginMeta parseHeaderMetadata(String code, String fallbackName) {
        PluginMeta meta = new PluginMeta();
        meta.name = findTag(code, "@name");
        if (meta.name == null) {
            meta.name = fallbackName.replace(".naw", "").replace(".js", "");
        }
        meta.id = findTag(code, "@id");
        if (meta.id == null) {
            meta.id = meta.name.toLowerCase().replaceAll("[^a-z0-9_.]", "_");
        }
        meta.version = findTag(code, "@version");
        if (meta.version == null) meta.version = "1.0.0";
        meta.author = findTag(code, "@author");
        if (meta.author == null) meta.author = "Пользователь";
        meta.description = findTag(code, "@description");
        if (meta.description == null) meta.description = "";
        return meta;
    }

    private static String findTag(String code, String tag) {
        Pattern pattern = Pattern.compile("\\" + tag + "\\s+([^\r\n*]+)");
        Matcher matcher = pattern.matcher(code);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static LoadedPlugin loadDexPlugin(Context context, File file, boolean enabled) {
        File optDir = new File(context.getCodeCacheDir(), "nawma_opt_dex");
        if (!optDir.exists()) {
            optDir.mkdirs();
        }

        try {
            DexClassLoader classLoader = new DexClassLoader(
                    file.getAbsolutePath(),
                    optDir.getAbsolutePath(),
                    null,
                    PluginLoader.class.getClassLoader()
            );

            PluginMeta meta = readMetadata(file);
            String mainClassName = meta.mainClass;
            if (mainClassName == null || mainClassName.isEmpty()) {
                mainClassName = findMainClass(file, classLoader);
            }

            if (mainClassName == null) {
                LoadedPlugin failed = new LoadedPlugin(file.getName(), file, null, classLoader, false);
                failed.setLoadError("Точка входа не найдена");
                return failed;
            }

            Class<?> clazz = classLoader.loadClass(mainClassName);
            if (!NawmaPlugin.class.isAssignableFrom(clazz)) {
                LoadedPlugin failed = new LoadedPlugin(file.getName(), file, null, classLoader, false);
                failed.setLoadError("Класс " + mainClassName + " не реализует NawmaPlugin");
                return failed;
            }

            NawmaPlugin instance = (NawmaPlugin) clazz.newInstance();
            String id = meta.id != null && !meta.id.trim().isEmpty() ? meta.id : instance.getId();
            if (id == null || id.trim().isEmpty()) id = file.getName();

            File pluginDir = new File(context.getFilesDir(), "nawma_plugin_data/" + id);
            PluginContext pluginContext = new PluginContext(context, id, pluginDir);
            instance.onLoad(pluginContext);

            LoadedPlugin loaded = new LoadedPlugin(id, file, instance, classLoader, enabled);
            if (enabled) {
                try {
                    instance.onEnable();
                } catch (Throwable t) {
                    FileLog.e(t);
                    loaded.setEnabled(false);
                    loaded.setLoadError("Ошибка запуска: " + t.getMessage());
                }
            }
            return loaded;
        } catch (Throwable t) {
            FileLog.e("[PluginLoader] Failed to load DEX: " + file.getName(), t);
            LoadedPlugin failed = new LoadedPlugin(file.getName(), file, null, null, false);
            failed.setLoadError("Ошибка загрузки DEX: " + t.getMessage());
            return failed;
        }
    }

    private static PluginMeta readMetadata(File file) {
        PluginMeta meta = new PluginMeta();
        try (ZipFile zip = new ZipFile(file)) {
            ZipEntry jsonEntry = zip.getEntry("plugin.json");
            if (jsonEntry != null) {
                try (InputStream is = zip.getInputStream(jsonEntry);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    JSONObject obj = new JSONObject(sb.toString());
                    meta.id = obj.optString("id", null);
                    meta.name = obj.optString("name", null);
                    meta.version = obj.optString("version", null);
                    meta.author = obj.optString("author", null);
                    meta.description = obj.optString("description", null);
                    meta.mainClass = obj.optString("main", null);
                    return meta;
                }
            }

            ZipEntry propEntry = zip.getEntry("plugin.properties");
            if (propEntry == null) propEntry = zip.getEntry("assets/plugin.properties");
            if (propEntry != null) {
                try (InputStream is = zip.getInputStream(propEntry)) {
                    Properties props = new Properties();
                    props.load(is);
                    meta.id = props.getProperty("id");
                    meta.name = props.getProperty("name");
                    meta.version = props.getProperty("version");
                    meta.author = props.getProperty("author");
                    meta.description = props.getProperty("description");
                    meta.mainClass = props.getProperty("main", props.getProperty("entry"));
                    return meta;
                }
            }
        } catch (Throwable ignored) {}
        return meta;
    }

    private static String findMainClass(File file, DexClassLoader classLoader) {
        for (String candidate : DEFAULT_CLASS_NAMES) {
            try {
                Class<?> c = classLoader.loadClass(candidate);
                if (NawmaPlugin.class.isAssignableFrom(c)) {
                    return candidate;
                }
            } catch (Throwable ignored) {}
        }

        try {
            DexFile dexFile = DexFile.loadDex(file.getAbsolutePath(), File.createTempFile("opt", "dex", file.getParentFile()).getAbsolutePath(), 0);
            Enumeration<String> entries = dexFile.entries();
            while (entries.hasMoreElements()) {
                String className = entries.nextElement();
                try {
                    Class<?> c = classLoader.loadClass(className);
                    if (NawmaPlugin.class.isAssignableFrom(c) && !c.isInterface()) {
                        return className;
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private static class PluginMeta {
        String id;
        String name;
        String version;
        String author;
        String description;
        String mainClass;
    }
}
