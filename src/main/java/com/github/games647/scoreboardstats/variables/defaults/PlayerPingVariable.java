package com.github.games647.scoreboardstats.variables.defaults;

import com.github.games647.scoreboardstats.variables.ReplaceEvent;
import com.google.common.collect.Lists;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Replace the ping variable.
 */
public class PlayerPingVariable extends DefaultReplaceAdapter<Plugin> {

    private Method getHandleMethod;
    private Field pingField;

    public PlayerPingVariable() {
        super(null, "ping");
    }

    @Override
    public void onReplace(Player player, String variable, ReplaceEvent replaceEvent) {
        replaceEvent.setScore(getReflectionPing(player));
    }

    private int getReflectionPing(Player player) {
        try {
            if (getHandleMethod == null) {
                getHandleMethod = player.getClass().getDeclaredMethod("getHandle");
                //disable java security check. This will speed it a little
                getHandleMethod.setAccessible(true);
            }

            Object entityPlayer = getHandleMethod.invoke(player);

            if (pingField == null) {
                if (isModdedServer()) {
                    //MCPC has a remapper, but it doesn't work if we get the class dynamic
                    setMCPCPing(entityPlayer);
                } else {
                    pingField = entityPlayer.getClass().getDeclaredField("ping");
                    //disable java security check. This will speed it a little
                    pingField.setAccessible(true);
                }
            }

            //returns the found int value
            return pingField.getInt(entityPlayer);
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException ex) {
            //Forward the exception to replaceManager
            throw new RuntimeException(ex);
        }
    }

    private static boolean isModdedServer() {
        //aggressive checking for modded servers
        List<String> checkVersions = Lists.newArrayList(Bukkit.getVersion(), Bukkit.getName()
                , Bukkit.getServer().toString());

        return checkVersions.stream().anyMatch(version -> (version.contains("MCPC") || version.contains("Cauldron")));
    }

    private void setMCPCPing(Object entityPlayer) {
        getPlugin().getLogger().info("Looking for ping field");

        //this isn't a secure, because it detects the ping variable by the ordering
        //a remaping (deobfuscate the variables) would work, but it won't be forwardcompatible
        Class<?> lastType = null;
        Field lastIntField = null;
        for (Field field : entityPlayer.getClass().getDeclaredFields()) {
            if (field.getType() == Integer.TYPE
                    && Modifier.isPublic(field.getModifiers())
                    && lastType == Boolean.TYPE) {
                lastIntField = field;
                continue;
            }

            if (field.getType() == Boolean.TYPE && lastIntField != null) {
                pingField = lastIntField;
                //disable java security check. This will speed it a little
                pingField.setAccessible(true);
                break;
            }

            lastIntField = null;
            lastType = field.getType();
        }

        getPlugin().getLogger().log(Level.INFO, "Ping field {0} for {1}", new Object[] {lastIntField, entityPlayer});
    }
}
