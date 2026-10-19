package org.telegram.messenger.nawmagram.plugins;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import org.mozilla.javascript.Function;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Global JavaScript bridge exposed to plugins as the "nawma" object.
 */
public class NawmaJsBridge {

    private final JsNawmaPlugin plugin;
    private final PluginContext pluginContext;
    private final Map<String, Function> eventHandlers = new HashMap<>();

    public NawmaJsBridge(JsNawmaPlugin plugin, PluginContext pluginContext) {
        this.plugin = plugin;
        this.pluginContext = pluginContext;
    }

    public String getPluginId() {
        return plugin.getId();
    }

    public String getPluginName() {
        return plugin.getName();
    }

    public void showToast(final String message) {
        if (pluginContext != null) {
            pluginContext.showToast(message);
        } else {
            AndroidUtilities.runOnUIThread(() -> {
                Context ctx = ApplicationLoader.applicationContext;
                if (ctx != null) {
                    android.widget.Toast.makeText(ctx, message, android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public void log(String message) {
        FileLog.d("[NawmaJS:" + plugin.getId() + "] " + message);
    }

    public void copyToClipboard(String text) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                ClipboardManager cm = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    ClipData clip = ClipData.newPlainText("nawma", text);
                    cm.setPrimaryClip(clip);
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        });
    }

    public void sendMessage(long dialogId, String text) {
        if (text == null || text.isEmpty()) return;
        AndroidUtilities.runOnUIThread(() -> {
            try {
                SendMessagesHelper.getInstance(UserConfig.selectedAccount).sendMessage(
                        SendMessagesHelper.SendMessageParams.of(text, dialogId)
                );
            } catch (Throwable t) {
                FileLog.e(t);
            }
        });
    }

    public void on(String eventName, Function handler) {
        if (eventName != null && handler != null) {
            eventHandlers.put(eventName.toLowerCase(), handler);
        }
    }

    public Function getHandler(String eventName) {
        return eventHandlers.get(eventName.toLowerCase());
    }

    public void setStorage(String key, String value) {
        if (pluginContext != null) {
            SharedPreferences sp = pluginContext.getPreferences();
            sp.edit().putString(key, value).apply();
        }
    }

    public String getStorage(String key, String defaultValue) {
        if (pluginContext != null) {
            return pluginContext.getPreferences().getString(key, defaultValue);
        }
        return defaultValue;
    }

    public void httpGet(final String urlString, final Function callback) {
        new Thread(() -> {
            String result = "";
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine).append("\n");
                }
                in.close();
                result = response.toString();
            } catch (Throwable t) {
                FileLog.e(t);
                result = "ERROR: " + t.getMessage();
            }

            final String finalResult = result;
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> {
                    plugin.callJsFunction(callback, new Object[]{finalResult});
                });
            }
        }).start();
    }
}
