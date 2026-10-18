package org.telegram.messenger.nawmagram.plugins;

import android.widget.Toast;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

/**
 * Built-in sample plugin demonstrating GUI modifications, custom commands and automation.
 */
public class SampleNawmaPlugin extends BasePlugin {

    @Override
    public String getId() {
        return "org.nawmagram.sample";
    }

    @Override
    public String getName() {
        return "Nawma Core Essentials";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getAuthor() {
        return "Nawmagram Team";
    }

    @Override
    public String getDescription() {
        return "Пример плагина: кастомные команды (.ping, .help) и улучшение интерфейса чатов.";
    }

    @Override
    public void onEnable() {
        if (context != null) {
            context.log("Nawma Core Essentials включен!");
        }
    }

    @Override
    public void onDisable() {
        if (context != null) {
            context.log("Nawma Core Essentials отключен.");
        }
    }

    @Override
    public void onActionBarCreated(ActionBar actionBar, BaseFragment fragment) {
        if (actionBar != null && fragment != null) {
            // Example GUI hook: subtle indicator or customization
        }
    }

    @Override
    public boolean onCommand(String command, String[] args, long dialogId, BaseFragment fragment) {
        if ("ping".equalsIgnoreCase(command)) {
            long now = System.currentTimeMillis();
            String response = "🏓 **Pong!**\n⚡ Плагин-движок Nawmagram работает стабильно.\n🕒 Отклик UI: мгновенно.";
            SendMessagesHelper.getInstance(UserConfig.selectedAccount).sendMessage(
                    SendMessagesHelper.SendMessageParams.of(response, dialogId)
            );
            return true;
        } else if ("help".equalsIgnoreCase(command)) {
            String helpText = "🚀 **Nawmagram Плагины активны**\n\nДоступные команды:\n• `.ping` — проверка работы плагинов\n• `.help` — справка по командам";
            SendMessagesHelper.getInstance(UserConfig.selectedAccount).sendMessage(
                    SendMessagesHelper.SendMessageParams.of(helpText, dialogId)
            );
            return true;
        }
        return false;
    }
}
