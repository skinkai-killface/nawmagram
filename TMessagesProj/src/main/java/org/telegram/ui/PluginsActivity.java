package org.telegram.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.nawmagram.plugins.LoadedPlugin;
import org.telegram.messenger.nawmagram.plugins.PluginManager;
import org.telegram.ui.ActionBar.AlertDialog;
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
    private static final int ID_RELOAD_BUTTON = 1001;
    private static final int ID_PLUGIN_BASE = 2000;

    @Override
    protected CharSequence getTitle() {
        return "Плагины Nawmagram";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader("Управление"));
        items.add(UItem.asSettingsCell(ID_INSTALL_BUTTON, R.drawable.menu_folder_add, "Установить плагин (.naw)", "Официальный формат .naw (или .dex, .apk)"));
        items.add(UItem.asSettingsCell(ID_RELOAD_BUTTON, R.drawable.settings_data, "Перезагрузить все плагины", "Повторное сканирование папки плагинов"));
        items.add(UItem.asShadow("Нажмите для выбора пакета .naw или .dex плагина с устройства."));

        List<LoadedPlugin> plugins = PluginManager.getInstance().getLoadedPlugins();
        items.add(UItem.asHeader("Установленные плагины (" + plugins.size() + ")"));

        if (plugins.isEmpty()) {
            items.add(UItem.asSettingsCell(-1, R.drawable.managed_bot, "Нет установленных плагинов", "Установите пакет .naw или .dex файл"));
        } else {
            for (int i = 0; i < plugins.size(); i++) {
                LoadedPlugin p = plugins.get(i);
                int itemId = ID_PLUGIN_BASE + i;

                String subtitle = "v" + p.getVersion() + " • " + p.getAuthor();
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
        items.add(UItem.asShadow("Пакет .naw — это ZIP-архив с classes.dex и метаданными plugin.json.
Также поддерживаются скомпилированные .dex и .apk файлы.\n\n"
                + "Хуки GUI позволяют изменять цвета тем, внедрять элементы в ActionBar и кастомизировать чаты.\n\n"
                + "Команды чата (.ping, .help) перехватываются до отправки на сервер Telegram."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_INSTALL_BUTTON) {
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

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Выберите плагин (.naw, .dex, .apk)"), REQUEST_PICK_PLUGIN);
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
            if (fileName == null || (!fileName.endsWith(".dex") && !fileName.endsWith(".apk") && !fileName.endsWith(".jar"))) {
                fileName = "plugin_" + System.currentTimeMillis() + ".dex";
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
        builder.setPositiveButton("Удалить", (dialog, which) -> {
            PluginManager.getInstance().deletePlugin(plugin.getId());
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
            Toast.makeText(getParentActivity(), "Плагин удалён", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Отмена", null);
        showDialog(builder.create());
    }
}
