package com.github.games647.scoreboardstats.variables;

import java.util.Iterator;
import java.util.Map;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

/**
 * Keeps track of plugin disables and enables. It will register default replacers
 * back again or removes replacers of disabled plugins.
 */
public class PluginListener implements Listener {

    private final ReplaceManager replaceManager;

    public PluginListener(ReplaceManager replaceManager) {
        this.replaceManager = replaceManager;
    }

    /**
     * Check for disabled plugin to re add the associated replacer
     *
     * @param enableEvent the enable event
     */
    @EventHandler
    public void onPluginEnable(PluginEnableEvent enableEvent) {
        //Register the listener back again if the plugin is available
        String enablePluginName = enableEvent.getPlugin().getName();
        Map<Class<? extends VariableReplaceAdapter<?>>, String> defaults = replaceManager.getDefaults();
        defaults.entrySet().stream().forEach(entry -> {
            String pluginName = entry.getValue();
            if (enablePluginName.equals(entry.getValue())) {
                replaceManager.registerDefault(entry.getKey(), pluginName);
            }
        });
    }

    /**
     * Check for disabled plugin to remove the associated replacer
     *
     * @param disableEvent the disable event
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent disableEvent) {
        //Remove the listener if the associated plugin was disabled
        String disablePluginName = disableEvent.getPlugin().getName();

        Map<String, VariableReplaceAdapter<? extends Plugin>> specificReplacers = replaceManager.getReplacers();
        Iterator<VariableReplaceAdapter<? extends Plugin>> iterator = specificReplacers.values().iterator();
        while (iterator.hasNext()) {
            Plugin plugin = iterator.next().getPlugin();
            if (plugin != null && plugin.getName().equals(disablePluginName)) {
                iterator.remove();
            }
        }
    }
}
