package org.telegram.messenger.nawmagram.plugins;

import java.io.File;

/**
 * Encapsulates a plugin loaded by Nawmagram.
 */
public class LoadedPlugin {

    private final String id;
    private final File sourceFile;
    private final NawmaPlugin instance;
    private final ClassLoader classLoader;
    private boolean enabled;
    private String loadError;

    public LoadedPlugin(String id, File sourceFile, NawmaPlugin instance, ClassLoader classLoader, boolean enabled) {
        this.id = id;
        this.sourceFile = sourceFile;
        this.instance = instance;
        this.classLoader = classLoader;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public NawmaPlugin getInstance() {
        return instance;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isBuiltIn() {
        return sourceFile == null;
    }

    public String getLoadError() {
        return loadError;
    }

    public void setLoadError(String loadError) {
        this.loadError = loadError;
    }

    public String getName() {
        return instance != null ? instance.getName() : id;
    }

    public String getVersion() {
        return instance != null ? instance.getVersion() : "1.0.0";
    }

    public String getAuthor() {
        return instance != null ? instance.getAuthor() : "Unknown";
    }

    public String getDescription() {
        return instance != null ? instance.getDescription() : "";
    }
}
