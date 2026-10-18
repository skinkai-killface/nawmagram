package org.telegram.messenger.nawmagram.plugins;

import android.content.Context;
import android.view.View;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;

/**
 * Interface that all Nawmagram plugins must implement.
 */
public interface NawmaPlugin {

    /**
     * Unique identifier of the plugin, e.g. "org.nawmagram.sample"
     */
    String getId();

    /**
     * Human-readable plugin name
     */
    String getName();

    /**
     * Plugin version string, e.g. "1.0.0"
     */
    String getVersion();

    /**
     * Plugin author
     */
    String getAuthor();

    /**
     * Short description of plugin capabilities
     */
    String getDescription();

    /**
     * Called when the plugin is loaded into memory.
     */
    void onLoad(PluginContext context);

    /**
     * Called when the plugin is enabled by the user or on startup.
     */
    void onEnable();

    /**
     * Called when the plugin is disabled.
     */
    void onDisable();

    /**
     * Called when the plugin is unloaded.
     */
    void onUnload();

    // ================= GUI Hooks =================

    /**
     * Intercept or replace a theme color key.
     * Return null to leave default color, or return 0xAARRGGBB color int.
     */
    Integer onModifyThemeColor(int colorKey, int defaultColor);

    /**
     * Called after an ActionBar is created in a fragment.
     * Plugins can add buttons, subtitles, custom views, or change styling.
     */
    void onActionBarCreated(ActionBar actionBar, BaseFragment fragment);

    /**
     * Called after a fragment's main view is created.
     */
    void onFragmentCreateView(BaseFragment fragment, View fragmentView);

    // ================= Command & Automation Hooks =================

    /**
     * Called when the user enters text in chat.
     * Return true if the command was processed (prevents message from sending to Telegram).
     */
    boolean onCommand(String command, String[] args, long dialogId, BaseFragment fragment);

    /**
     * Intercept outgoing message text.
     * Return modified text, or original text. Return null to cancel message sending.
     */
    String onMessageSend(long dialogId, String text);

    /**
     * Intercept incoming message for automation/processing.
     */
    void onMessageReceived(MessageObject message);

    /**
     * Optional settings view for the plugin. Return null if none.
     */
    View createSettingsView(Context context);
}
