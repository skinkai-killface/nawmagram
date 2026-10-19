package org.telegram.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.nawmagram.plugins.JsNawmaPlugin;
import org.telegram.messenger.nawmagram.plugins.LoadedPlugin;
import org.telegram.messenger.nawmagram.plugins.PluginManager;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen for managing, enabling/disabling, and installing Nawmagram plugins.
 */
public class PluginsActivity extends UniversalFragment {

    private static final int REQUEST_PICK_PLUGIN = 2001;
    private static final int ID_INSTALL_BUTTON = 1000;
    private static final int ID_PASTE_BUTTON = 1001;
    private static final int ID_RELOAD_BUTTON = 1002;
    private static final int ID_PLUGIN_BASE = 2000;

    @Override
    protected CharSequence getTitle() {
        return "Плагины Nawmagram";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader("Управление"));
        items.add(UItem.asSettingsCell(ID_PASTE_BUTTON, R.drawable.msg_edit, "Создать / Вставить код плагина", "Вставьте JS-код, сгенерированный любым ИИ"));
        items.add(UItem.asSettingsCell(ID_INSTALL_BUTTON, R.drawable.menu_folder_add, "Установить плагин из файла", "Поддерживаются файлы .naw, .js, .dex"));
        items.add(UItem.asSettingsCell(ID_RELOAD_BUTTON, R.drawable.settings_data, "Перезагрузить все плагины", "Повторное сканирование папки плагинов"));
        items.add(UItem.asShadow("Вы можете скопировать код от ChatGPT/Claude и вставить его напрямую кнопкой выше."));

        List<LoadedPlugin> plugins = PluginManager.getInstance().getLoadedPlugins();
        items.add(UItem.asHeader("Установленные плагины (" + plugins.size() + ")"));

        if (plugins.isEmpty()) {
            items.add(UItem.asSettingsCell(-1, R.drawable.managed_bot, "Нет установленных плагинов", "Нажмите «Создать / Вставить код» для добавления"));
        } else {
            for (int i = 0; i < plugins.size(); i++) {
                LoadedPlugin p = plugins.get(i);
                int itemId = ID_PLUGIN_BASE + i;

                String typeBadge = (p.getInstance() instanceof JsNawmaPlugin) ? "[JS] " : "[DEX] ";
                String subtitle = typeBadge + "v" + p.getVersion() + " • " + p.getAuthor();
                if (p.getLoadError() != null) {
                    subtitle = "⚠️ Ошибка: " + p.getLoadError();
                } else if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                    subtitle += " — " + p.getDescription();
                }

                items.add(UItem.asCheck(itemId, p.getName()).setChecked(p.isEnabled()));
                items.add(UItem.asShadow(subtitle));
            }
        }

        items.add(UItem.asHeader("Справка для разработчиков"));
        items.add(UItem.asShadow("Плагины Nawmagram пишутся на обычном JavaScript!\n\n"
                + "Вы можете попросить любой ИИ написать скрипт с использованием объекта nawma (nawma.showToast, nawma.on, nawma.sendMessage, и др.).\n\n"
                + "Также сохраняется поддержка бинарных пакетов .dex и .naw ZIP-архивов."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_PASTE_BUTTON) {
            showCreatePluginDialog();
        } else if (item.id == ID_INSTALL_BUTTON) {
            openFilePicker();
        } else if (item.id == ID_RELOAD_BUTTON) {
            PluginManager.getInstance().reloadPlugins();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            Toast.makeText(getParentActivity(), "Плагины перезагружены", Toast.LENGTH_SHORT).show();
        } else if (item.id >= ID_PLUGIN_BASE) {
            int index = item.id - ID_PLUGIN_BASE;
            List<LoadedPlugin> plugins = PluginManager.getInstance().getLoadedPlugins();
            if (index >= 0 && index < plugins.size()) {
                LoadedPlugin p = plugins.get(index);
                PluginManager.getInstance().setPluginEnabled(p.getId(), !p.isEnabled());
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= ID_PLUGIN_BASE) {
            int index = item.id - ID_PLUGIN_BASE;
            List<LoadedPlugin> plugins = PluginManager.getInstance().getLoadedPlugins();
            if (index >= 0 && index < plugins.size()) {
                LoadedPlugin p = plugins.get(index);
                if (p.isBuiltIn()) {
                    Toast.makeText(getParentActivity(), "Встроенный плагин нельзя удалить", Toast.LENGTH_SHORT).show();
                    return true;
                }
                showPluginActionsDialog(p);
                return true;
            }
        }
        return false;
    }

    private void showCreatePluginDialog() {
        if (getParentActivity() == null) return;

        String clipText = "";
        try {
            ClipboardManager cm = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence cs = cm.getPrimaryClip().getItemAt(0).getText();
                if (cs != null) {
                    String candidate = cs.toString().trim();
                    if (candidate.contains("nawma") || candidate.contains("function") || candidate.startsWith("/**")) {
                        clipText = candidate;
                    }
                }
            }
        } catch (Throwable ignored) {}

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Создать JS-плагин");

        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));

        final EditText nameInput = new EditText(getParentActivity());
        nameInput.setHint("Имя файла (например, my_plugin.naw)");
        nameInput.setText("custom_plugin.naw");
        nameInput.setTextSize(14);
        nameInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(nameInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        final EditText codeInput = new EditText(getParentActivity());
        codeInput.setHint("// Вставьте сюда JavaScript код плагина от ИИ\n/**\n * @name Мой плагин\n * @description Описание\n */\n\nnawma.on('message_received', function(msg) {\n    nawma.showToast('Новое сообщение: ' + msg.text);\n});");
        if (!clipText.isEmpty()) {
            codeInput.setText(clipText);
        }
        codeInput.setTextSize(13);
        codeInput.setTypeface(Typeface.MONOSPACE);
        codeInput.setGravity(Gravity.TOP | Gravity.START);
        codeInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        codeInput.setLines(10);
        codeInput.setMaxLines(20);

        ScrollView scrollView = new ScrollView(getParentActivity());
        scrollView.addView(codeInput);
        layout.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(220)));

        builder.setView(layout);
        builder.setPositiveButton("Запустить", (dialog, which) -> {
            String fileName = nameInput.getText().toString().trim();
            String code = codeInput.getText().toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(getParentActivity(), "Код плагина не может быть пустым", Toast.LENGTH_SHORT).show();
                return;
            }
            if (fileName.isEmpty()) {
                fileName = "plugin_" + System.currentTimeMillis() + ".naw";
            }
            boolean success = PluginManager.getInstance().savePluginCode(fileName, code);
            if (success) {
                Toast.makeText(getParentActivity(), "Плагин успешно сохранён и запущен!", Toast.LENGTH_LONG).show();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            } else {
                Toast.makeText(getParentActivity(), "Ошибка сохранения плагина", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        showDialog(builder.create());
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Выберите плагин (.naw, .js, .dex)"), REQUEST_PICK_PLUGIN);
        } catch (Throwable t) {
            FileLog.e(t);
            Toast.makeText(getParentActivity(), "Не удалось открыть файловый менеджер", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_PLUGIN && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            installFromUri(uri);
        }
    }

    private void installFromUri(Uri uri) {
        try {
            String fileName = getFileName(uri);
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "plugin_" + System.currentTimeMillis() + ".naw";
            }

            InputStream is = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);
            boolean success = PluginManager.getInstance().installPlugin(is, fileName);
            if (success) {
                Toast.makeText(getParentActivity(), "Плагин успешно установлен!", Toast.LENGTH_LONG).show();
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            } else {
                Toast.makeText(getParentActivity(), "Не удалось загрузить плагин. Проверьте формат файла.", Toast.LENGTH_LONG).show();
            }
        } catch (Throwable t) {
            FileLog.e(t);
            Toast.makeText(getParentActivity(), "Ошибка установки: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = ApplicationLoader.applicationContext.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    private void showPluginActionsDialog(LoadedPlugin plugin) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(plugin.getName());
        builder.setMessage("Автор: " + plugin.getAuthor() + "\nВерсия: " + plugin.getVersion() + "\n\n" + plugin.getDescription());

        if (plugin.getInstance() instanceof JsNawmaPlugin) {
            final String code = ((JsNawmaPlugin) plugin.getInstance()).getSourceCode();
            builder.setNeutralButton("Копировать код", (dialog, which) -> {
                try {
                    ClipboardManager cm = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("plugin_code", code));
                        Toast.makeText(getParentActivity(), "Код скопирован в буфер", Toast.LENGTH_SHORT).show();
                    }
                } catch (Throwable ignored) {}
            });
        }

        builder.setPositiveButton("Удалить", (dialog, which) -> {
            PluginManager.getInstance().deletePlugin(plugin.getId());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            Toast.makeText(getParentActivity(), "Плагин удалён", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Закрыть", null);
        showDialog(builder.create());
    }
}
