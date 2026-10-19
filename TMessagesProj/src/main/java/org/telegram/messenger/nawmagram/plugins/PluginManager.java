package org.telegram.messenger.nawmagram.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central orchestrator for Nawmagram plugin discovery, lifecycle, and hook dispatching.
 */
public class PluginManager {

    private static final String TAG = "[PluginManager]";
    private static final String PREFS_NAME = "nawmagram_plugins_state";
    private static volatile PluginManager instance;

    private Context context;
    private SharedPreferences prefs;
    private File pluginsDir;
    private final Map<String, LoadedPlugin> plugins = new LinkedHashMap<>();
    private boolean initialized = false;

    private PluginManager() {}

    public static PluginManager getInstance() {
        if (instance == null) {
            synchronized (PluginManager.class) {
                if (instance == null) {
                    instance = new PluginManager();
                }
            }
        }
        return instance;
    }

    public synchronized void init(Context context) {
        if (initialized || context == null) {
            return;
        }
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.pluginsDir = new File(this.context.getFilesDir(), "nawma_plugins");
        if (!pluginsDir.exists()) {
            pluginsDir.mkdirs();
        }

        // Register built-in sample plugin
        registerBuiltIn(new SampleNawmaPlugin());

        // Scan and load external .dex/.apk/.jar plugins
        reloadPlugins();
        initialized = true;
        FileLog.d(TAG + " Initialized. Total plugins: " + plugins.size());
    }

    public synchronized void reloadPlugins() {
        if (context == null || pluginsDir == null) {
            return;
        }

        // Keep built-ins, reload file-based
        List<String> toRemove = new ArrayList<>();
        for (LoadedPlugin p : plugins.values()) {
            if (!p.isBuiltIn()) {
                if (p.isEnabled() && p.getInstance() != null) {
                    try {
                        p.getInstance().onDisable();
                    } catch (Throwable ignored) {}
                }
                toRemove.add(p.getId());
            }
        }
        for (String id : toRemove) {
            plugins.remove(id);
        }

        File[] files = pluginsDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".naw") || lower.endsWith(".js") || lower.endsWith(".dex") || lower.endsWith(".apk") || lower.endsWith(".jar");
        });

        if (files != null) {
            for (File file : files) {
                try {
                    String baseName = file.getName();
                    boolean enabled = prefs.getBoolean("plugin_enabled_" + baseName, true);
                    LoadedPlugin loaded = PluginLoader.loadPlugin(context, file, enabled);
                    if (loaded != null) {
                        plugins.put(loaded.getId(), loaded);
                        FileLog.d(TAG + " Loaded external plugin: " + loaded.getName() + " (" + loaded.getId() + ")");
                    }
                } catch (Throwable t) {
                    FileLog.e(TAG + " Failed loading plugin file " + file.getName(), t);
                }
            }
        }
    }

    private void registerBuiltIn(NawmaPlugin plugin) {
        if (plugin == null) return;
        String id = plugin.getId();
        boolean enabled = prefs.getBoolean("plugin_enabled_" + id, true);
        PluginContext ctx = new PluginContext(context, id, new File(context.getFilesDir(), "nawma_plugin_data/" + id));
        plugin.onLoad(ctx);
        if (enabled) {
            try {
                plugin.onEnable();
            } catch (Throwable t) {
                FileLog.e(TAG + " Error enabling built-in plugin " + id, t);
                enabled = false;
            }
        }
        plugins.put(id, new LoadedPlugin(id, null, plugin, getClass().getClassLoader(), enabled));
    }

    public synchronized List<LoadedPlugin> getLoadedPlugins() {
        return Collections.unmodifiableList(new ArrayList<>(plugins.values()));
    }

    public synchronized boolean isPluginEnabled(String pluginId) {
        LoadedPlugin plugin = plugins.get(pluginId);
        return plugin != null && plugin.isEnabled();
    }

    public synchronized void setPluginEnabled(String pluginId, boolean enabled) {
        LoadedPlugin plugin = plugins.get(pluginId);
        if (plugin == null || plugin.isEnabled() == enabled) {
            return;
        }

        plugin.setEnabled(enabled);
        String prefKey = plugin.isBuiltIn() ? "plugin_enabled_" + pluginId : "plugin_enabled_" + plugin.getSourceFile().getName();
        prefs.edit().putBoolean(prefKey, enabled).apply();

        if (plugin.getInstance() != null) {
            try {
                if (enabled) {
                    plugin.getInstance().onEnable();
                } else {
                    plugin.getInstance().onDisable();
                }
            } catch (Throwable t) {
                FileLog.e(TAG + " Error toggling plugin " + pluginId, t);
            }
        }
    }

    public synchronized boolean installPlugin(InputStream inputStream, String fileName) {
        if (inputStream == null || fileName == null || pluginsDir == null) {
            return false;
        }
        File targetFile = new File(pluginsDir, fileName);
        try (OutputStream os = new FileOutputStream(targetFile)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = inputStream.read(buf)) != -1) {
                os.write(buf, 0, read);
            }
            os.flush();

            LoadedPlugin loaded = PluginLoader.loadPlugin(context, targetFile, true);
            if (loaded != null) {
                plugins.put(loaded.getId(), loaded);
                prefs.edit().putBoolean("plugin_enabled_" + targetFile.getName(), true).apply();
                return true;
            }
        } catch (Throwable t) {
            FileLog.e(TAG + " Error installing plugin " + fileName, t);
        }
        return false;
    }

    public synchronized boolean savePluginCode(String fileName, String code) {
        if (code == null || fileName == null || pluginsDir == null) {
            return false;
        }
        if (!fileName.endsWith(".naw") && !fileName.endsWith(".js")) {
            fileName += ".naw";
        }
        File targetFile = new File(pluginsDir, fileName);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(targetFile)) {
            fos.write(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            fos.flush();

            LoadedPlugin loaded = PluginLoader.loadPlugin(context, targetFile, true);
            if (loaded != null) {
                plugins.put(loaded.getId(), loaded);
                prefs.edit().putBoolean("plugin_enabled_" + targetFile.getName(), true).apply();
                return true;
            }
        } catch (Throwable t) {
            FileLog.e(TAG + " Error saving plugin code " + fileName, t);
        }
        return false;
    }

    public synchronized boolean deletePlugin(String pluginId) {
        LoadedPlugin plugin = plugins.get(pluginId);
        if (plugin == null || plugin.isBuiltIn()) {
            return false;
        }
        if (plugin.isEnabled() && plugin.getInstance() != null) {
            try {
                plugin.getInstance().onDisable();
                plugin.getInstance().onUnload();
            } catch (Throwable ignored) {}
        }
        if (plugin.getSourceFile() != null && plugin.getSourceFile().exists()) {
            plugin.getSourceFile().delete();
        }
        plugins.remove(pluginId);
        prefs.edit().remove("plugin_enabled_" + plugin.getSourceFile().getName()).apply();
        return true;
    }

    // ================= Dispatchers =================

    public Integer dispatchThemeColor(int colorKey, int defaultColor) {
        if (!initialized) return null;
        for (LoadedPlugin p : plugins.values()) {
            if (p.isEnabled() && p.getInstance() != null) {
                try {
                    Integer override = p.getInstance().onModifyThemeColor(colorKey, defaultColor);
                    if (override != null) {
                        return override;
                    }
                } catch (Throwable t) {
                    FileLog.e(TAG + " Plugin " + p.getId() + " onModifyThemeColor crashed", t);
                }
            }
        }
        return null;
    }

    public void dispatchActionBarCreated(ActionBar actionBar, BaseFragment fragment) {
        if (!initialized) return;
        for (LoadedPlugin p : plugins.values()) {
            if (p.isEnabled() && p.getInstance() != null) {
                try {
                    p.getInstance().onActionBarCreated(actionBar, fragment);
                } catch (Throwable t) {
                    FileLog.e(TAG + " Plugin " + p.getId() + " onActionBarCreated crashed", t);
                }
            }
        }
    }

    public void dispatchFragmentCreateView(BaseFragment fragment, View fragmentView) {
        if (!initialized) return;
        for (LoadedPlugin p : plugins.values()) {
            if (p.isEnabled() && p.getInstance() != null) {
                try {
                    p.getInstance().onFragmentCreateView(fragment, fragmentView);
                } catch (Throwable t) {
                    FileLog.e(TAG + " Plugin " + p.getId() + " onFragmentCreateView crashed", t);
                }
            }
        }
    }

    public boolean dispatchCommand(String fullText, long dialogId, BaseFragment fragment) {
        if (!initialized || fullText == null) return false;
        String text = fullText.trim();
        if (!text.startsWith(".") && !text.startsWith("!")) {
            return false;
        }

        String withoutPrefix = text.substring(1).trim();
        if (withoutPrefix.isEmpty()) return false;

        String[] parts = withoutPrefix.split("\\s+");
        String command = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        for (LoadedPlugin p : plugins.values()) {
            if (p.isEnabled() && p.getInstance() != null) {
                try {
                    if (p.getInstance().onCommand(command, args, dialogId, fragment)) {
                        return true;
                    }
                } catch (Throwable t) {
                    FileLog.e(TAG + " Plugin " + p.getId() + " onCommand crashed", t);
                }
            }
        }
        return false;
    }

    public String dispatchMessageSend(long dialogId, String text) {
        if (!initialized || text == null) return text;
        String current = text;
        for (LoadedPlugin p : plugins.values()) {
            if (p.isEnabled() && p.getInstance() != null) {
                try {
                    current = p.getInstance().onMessageSend(dialogId, current);
                    if (current == null) {
                        return null; // Message cancelled
                    }
                } catch (Throwable t) {
                    FileLog.e(TAG + " Plugin " + p.getId() + " onMessageSend crashed", t);
                }
            }
        }
        return current;
    }

    public void dispatchMessageReceived(MessageObject message) {
        if (!initialized || message == null) return;
        for (LoadedPlugin p : plugins.values()) {
            if (p.isEnabled() && p.getInstance() != null) {
                try {
                    p.getInstance().onMessageReceived(message);
                } catch (Throwable t) {
                    FileLog.e(TAG + " Plugin " + p.getId() + " onMessageReceived crashed", t);
                }
            }
        }
    }
}
