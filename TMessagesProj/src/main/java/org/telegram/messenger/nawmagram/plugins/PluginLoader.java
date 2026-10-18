package org.telegram.messenger.nawmagram.plugins;

import android.content.Context;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads .naw, .dex, .apk, and .jar plugins using DexClassLoader.
 * Supports plugin.json and plugin.properties metadata.
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

            // Read metadata (plugin.json or plugin.properties)
            PluginMeta meta = readMetadata(file);

            String mainClassName = meta.mainClass;
            if (mainClassName == null || mainClassName.isEmpty()) {
                mainClassName = findMainClass(file, classLoader);
            }

            if (mainClassName == null) {
                FileLog.e("[PluginLoader] Could not find entry point in " + file.getName());
                LoadedPlugin failed = new LoadedPlugin(file.getName(), file, null, classLoader, false);
                failed.setLoadError("Точка входа не найдена (укажите \"main\" в plugin.json)");
                return failed;
            }

            Class<?> clazz = classLoader.loadClass(mainClassName);
            if (!NawmaPlugin.class.isAssignableFrom(clazz)) {
                FileLog.e("[PluginLoader] Class " + mainClassName + " does not implement NawmaPlugin");
                LoadedPlugin failed = new LoadedPlugin(file.getName(), file, null, classLoader, false);
                failed.setLoadError("Класс " + mainClassName + " не реализует NawmaPlugin");
                return failed;
            }

            NawmaPlugin instance = (NawmaPlugin) clazz.newInstance();
            String id = meta.id;
            if (id == null || id.trim().isEmpty()) {
                id = instance.getId();
            }
            if (id == null || id.trim().isEmpty()) {
                id = file.getName();
            }

            File pluginDir = new File(context.getFilesDir(), "nawma_plugin_data/" + id);
            PluginContext pluginContext = new PluginContext(context, id, pluginDir);
            instance.onLoad(pluginContext);

            LoadedPlugin loaded = new LoadedPlugin(id, file, instance, classLoader, enabled);
            if (enabled) {
                try {
                    instance.onEnable();
                } catch (Throwable t) {
                    FileLog.e("[PluginLoader] Error enabling plugin: " + id, t);
                    loaded.setEnabled(false);
                    loaded.setLoadError("Ошибка запуска: " + t.getMessage());
                }
            }
            return loaded;

        } catch (Throwable t) {
            FileLog.e("[PluginLoader] Failed to load plugin file: " + file.getName(), t);
            LoadedPlugin failed = new LoadedPlugin(file.getName(), file, null, null, false);
            failed.setLoadError("Ошибка загрузки: " + t.getMessage());
            return failed;
        }
    }

    private static PluginMeta readMetadata(File file) {
        PluginMeta meta = new PluginMeta();
        try (ZipFile zip = new ZipFile(file)) {
            // 1. Try plugin.json
            ZipEntry jsonEntry = zip.getEntry("plugin.json");
            if (jsonEntry != null) {
                try (InputStream is = zip.getInputStream(jsonEntry);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
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

            // 2. Try plugin.properties
            ZipEntry propEntry = zip.getEntry("plugin.properties");
            if (propEntry == null) {
                propEntry = zip.getEntry("assets/plugin.properties");
            }
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
        // 1. Try default candidate class names
        for (String candidate : DEFAULT_CLASS_NAMES) {
            try {
                Class<?> c = classLoader.loadClass(candidate);
                if (NawmaPlugin.class.isAssignableFrom(c)) {
                    return candidate;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Scan DexFile entries for any class implementing NawmaPlugin
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
