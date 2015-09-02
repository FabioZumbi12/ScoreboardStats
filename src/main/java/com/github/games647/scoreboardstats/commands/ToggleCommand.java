package com.github.games647.scoreboardstats.commands;

import com.github.games647.scoreboardstats.config.Lang;
import com.github.games647.scoreboardstats.RefreshTask;
import com.github.games647.scoreboardstats.ScoreboardStats;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;

/**
 * Change the visibility of the scoreboard
 */
public class ToggleCommand extends CommandHandler {

    private static final String TOGGLE_COMMAND = "toggle";

    public ToggleCommand(ScoreboardStats plugin) {
        super(TOGGLE_COMMAND, "&aToggles the sidebar", plugin, "hide", "show");
    }

    @Override
    public void onCommand(CommandSender sender, String subCommand, String... args) {
        if (!(sender instanceof Player)) {
            //the console cannot have a scoreboard
            sender.sendMessage(Lang.get("noConsole"));
            return;
        }

         //We checked that it can only be players
        final Player player = (Player) sender;
        final RefreshTask refreshTask = plugin.getRefreshTask();
        if (refreshTask.contains(player)) {
            if ("hide".equals(subCommand) || TOGGLE_COMMAND.equals(subCommand)) {
                refreshTask.remove(player);
                player.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
                player.sendMessage(Lang.get("onToggle"));
            }
        } else if ("show".equals(subCommand) || TOGGLE_COMMAND.equals(subCommand)) {
            player.sendMessage(Lang.get("onToggle"));
            refreshTask.addToQueue(player);
        }
    }
}
