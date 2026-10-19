package org.telegram.messenger.nawmagram.plugins;

import android.view.View;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;

/**
 * Executes a Nawmagram plugin written in JavaScript using Mozilla Rhino.
 */
public class JsNawmaPlugin implements NawmaPlugin {

    private final String id;
    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final String sourceCode;

    private PluginContext pluginContext;
    private NawmaJsBridge bridge;
    private Scriptable scope;

    public JsNawmaPlugin(String id, String name, String version, String author, String description, String sourceCode) {
        this.id = id != null && !id.isEmpty() ? id : "js.plugin." + System.currentTimeMillis();
        this.name = name != null && !name.isEmpty() ? name : "Безымянный JS-плагин";
        this.version = version != null && !version.isEmpty() ? version : "1.0.0";
        this.author = author != null && !author.isEmpty() ? author : "Пользователь";
        this.description = description != null ? description : "";
        this.sourceCode = sourceCode;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    @Override
    public void onLoad(PluginContext context) {
        this.pluginContext = context;
        this.bridge = new NawmaJsBridge(this, context);

        Context rhino = Context.enter();
        rhino.setOptimizationLevel(-1); // Critical for Android
        try {
            scope = rhino.initSafeStandardObjects();
            Object wrappedBridge = Context.javaToJS(bridge, scope);
            ScriptableObject.putProperty(scope, "nawma", wrappedBridge);

            if (sourceCode != null && !sourceCode.trim().isEmpty()) {
                rhino.evaluateString(scope, sourceCode, id + ".js", 1, null);
                FileLog.d("[JsNawmaPlugin] Evaluated JS code for: " + id);
            }
        } catch (Throwable t) {
            FileLog.e("[JsNawmaPlugin] Error evaluating script for " + id, t);
            if (context != null) {
                context.showToast("Ошибка JS-плагина " + name + ": " + t.getMessage());
            }
        } finally {
            Context.exit();
        }
    }

    @Override
    public void onEnable() {
        callHandler("enable", new Object[0]);
    }

    @Override
    public void onDisable() {
        callHandler("disable", new Object[0]);
    }

    @Override
    public void onUnload() {
        callHandler("unload", new Object[0]);
        scope = null;
    }

    @Override
    public Integer onModifyThemeColor(int colorKey, int defaultColor) {
        Object res = callHandler("theme_color", new Object[]{colorKey, defaultColor});
        if (res instanceof Number) {
            return ((Number) res).intValue();
        }
        return null;
    }

    @Override
    public void onActionBarCreated(ActionBar actionBar, BaseFragment fragment) {
        callHandler("action_bar", new Object[]{actionBar, fragment});
    }

    @Override
    public void onFragmentCreateView(BaseFragment fragment, View fragmentView) {
        callHandler("fragment_view", new Object[]{fragment, fragmentView});
    }

    @Override
    public boolean onCommand(String command, String[] args, long dialogId, BaseFragment fragment) {
        Object res = callHandler("command", new Object[]{command, args, dialogId, fragment});
        if (res instanceof Boolean) {
            return (Boolean) res;
        }
        return false;
    }

    @Override
    public String onMessageSend(long dialogId, String text) {
        Object res = callHandler("message_send", new Object[]{dialogId, text});
        if (res == null) {
            return text;
        }
        if (res instanceof Boolean && !((Boolean) res)) {
            return null; // Cancel message sending
        }
        return res.toString();
    }

    @Override
    public void onMessageReceived(MessageObject message) {
        if (message == null) return;
        try {
            NativeObject msgObj = new NativeObject();
            msgObj.put("id", msgObj, message.getId());
            msgObj.put("dialogId", msgObj, message.getDialogId());
            msgObj.put("text", msgObj, message.messageText != null ? message.messageText.toString() : "");
            msgObj.put("isOut", msgObj, message.isOut());
            msgObj.put("senderId", msgObj, message.getFromChatId());
            callHandler("message_received", new Object[]{msgObj});
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    @Override
    public View createSettingsView(android.content.Context context) {
        return null;
    }

    public Object callJsFunction(Function function, Object[] args) {
        if (function == null || scope == null) return null;
        Context rhino = Context.enter();
        rhino.setOptimizationLevel(-1);
        try {
            return function.call(rhino, scope, scope, args != null ? args : new Object[0]);
        } catch (Throwable t) {
            FileLog.e("[JsNawmaPlugin:" + id + "] Function execution error", t);
            return null;
        } finally {
            Context.exit();
        }
    }

    private Object callHandler(String eventName, Object[] args) {
        if (bridge == null) return null;
        Function handler = bridge.getHandler(eventName);
        if (handler != null) {
            return callJsFunction(handler, args);
        }
        return null;
    }
}
