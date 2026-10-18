package org.telegram.messenger.nawmagram.plugins;

import android.content.Context;
import android.view.View;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;

/**
 * Convenient base class with default implementations of all NawmaPlugin methods.
 */
public abstract class BasePlugin implements NawmaPlugin {

    protected PluginContext context;

    @Override
    public void onLoad(PluginContext context) {
        this.context = context;
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

    @Override
    public void onUnload() {}

    @Override
    public Integer onModifyThemeColor(int colorKey, int defaultColor) {
        return null;
    }

    @Override
    public void onActionBarCreated(ActionBar actionBar, BaseFragment fragment) {}

    @Override
    public void onFragmentCreateView(BaseFragment fragment, View fragmentView) {}

    @Override
    public boolean onCommand(String command, String[] args, long dialogId, BaseFragment fragment) {
        return false;
    }

    @Override
    public String onMessageSend(long dialogId, String text) {
        return text;
    }

    @Override
    public void onMessageReceived(MessageObject message) {}

    @Override
    public View createSettingsView(Context context) {
        return null;
    }
}
