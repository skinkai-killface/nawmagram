package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.messenger.nawmagram.plugins.PluginManager;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * Main settings screen for Nawmagram fork features and plugin management.
 */
public class NawmagramSettingsActivity extends UniversalFragment {

    private static final String PREFS_NAME = "nawmagram_prefs";
    public static final String KEY_HIDE_STORIES = "hide_stories";
    public static final String KEY_GRADIENT_BUBBLES = "gradient_bubbles";
    public static final String KEY_COMPACT_ACTIONBAR = "compact_actionbar";

    private static final int ID_PLUGINS = 1;
    private static final int ID_HIDE_STORIES = 2;
    private static final int ID_GRADIENT_BUBBLES = 3;
    private static final int ID_COMPACT_ACTIONBAR = 4;

    private SharedPreferences prefs;

    @Override
    public boolean onFragmentCreate() {
        prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return super.onFragmentCreate();
    }

    @Override
    protected CharSequence getTitle() {
        return "Настройки Nawmagram";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader("Расширения"));
        int pluginCount = PluginManager.getInstance().getLoadedPlugins().size();
        String pluginValue = pluginCount + " загружено";
        items.add(UItem.asSettingsCell(ID_PLUGINS, R.drawable.settings_features, "Плагины", pluginValue));
        items.add(UItem.asShadow("Модульная система для модификации интерфейса, автоматизации и скриптов."));

        items.add(UItem.asHeader("Интерфейс"));
        items.add(UItem.asCheck(ID_HIDE_STORIES, "Скрыть Истории (Stories)").setChecked(prefs.getBoolean(KEY_HIDE_STORIES, false)));
        items.add(UItem.asCheck(ID_GRADIENT_BUBBLES, "Градиентные пузырьки сообщений").setChecked(prefs.getBoolean(KEY_GRADIENT_BUBBLES, true)));
        items.add(UItem.asCheck(ID_COMPACT_ACTIONBAR, "Компактная шапка чатов").setChecked(prefs.getBoolean(KEY_COMPACT_ACTIONBAR, false)));
        items.add(UItem.asShadow("Кастомизация внешнего вида приложения и диалогов."));

        items.add(UItem.asHeader("О форке"));
        items.add(UItem.asSettingsCell(100, R.drawable.settings_policy, "Nawmagram v1.1", "Движок плагинов активен"));
        items.add(UItem.asShadow("Nawmagram — кастомный форк Telegram Android с поддержкой DEX-плагинов."));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id == ID_PLUGINS) {
            presentFragment(new PluginsActivity());
        } else if (item.id == ID_HIDE_STORIES) {
            boolean current = prefs.getBoolean(KEY_HIDE_STORIES, false);
            prefs.edit().putBoolean(KEY_HIDE_STORIES, !current).apply();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (item.id == ID_GRADIENT_BUBBLES) {
            boolean current = prefs.getBoolean(KEY_GRADIENT_BUBBLES, true);
            prefs.edit().putBoolean(KEY_GRADIENT_BUBBLES, !current).apply();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        } else if (item.id == ID_COMPACT_ACTIONBAR) {
            boolean current = prefs.getBoolean(KEY_COMPACT_ACTIONBAR, false);
            prefs.edit().putBoolean(KEY_COMPACT_ACTIONBAR, !current).apply();
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
