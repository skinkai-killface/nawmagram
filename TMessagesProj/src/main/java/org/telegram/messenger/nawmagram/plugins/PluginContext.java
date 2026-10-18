package org.telegram.messenger.nawmagram.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;

import java.io.File;

/**
 * Context provided to plugins for interacting with the Nawmagram environment.
 */
public class PluginContext {

    private final Context applicationContext;
    private final String pluginId;
    private final File pluginDir;

    public PluginContext(Context applicationContext, String pluginId, File pluginDir) {
        this.applicationContext = applicationContext;
        this.pluginId = pluginId;
        this.pluginDir = pluginDir;
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    public String getPluginId() {
        return pluginId;
    }

    public File getPluginDir() {
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        return pluginDir;
    }

    public SharedPreferences getPreferences() {
        return applicationContext.getSharedPreferences("nawma_plugin_" + pluginId, Context.MODE_PRIVATE);
    }

    public void showToast(final String message) {
        AndroidUtilities.runOnUIThread(() -> Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show());
    }

    public void log(String message) {
        FileLog.d("[Plugin:" + pluginId + "] " + message);
    }

    public void logError(String message, Throwable t) {
        FileLog.e("[Plugin:" + pluginId + "] " + message, t);
    }
}
