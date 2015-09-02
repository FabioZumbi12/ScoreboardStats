package com.github.games647.scoreboardstats;

import com.avaje.ebean.EbeanServer;
import com.comphenix.protocol.ProtocolLibrary;
import com.github.games647.scoreboardstats.config.Lang;
import com.github.games647.scoreboardstats.config.Settings;
import com.github.games647.scoreboardstats.commands.SidebarCommands;
import com.github.games647.scoreboardstats.Updater.UpdateCallback;
import com.github.games647.scoreboardstats.Updater.UpdateResult;
import com.github.games647.scoreboardstats.pvpstats.Database;
import com.github.games647.scoreboardstats.pvpstats.PlayerStats;
import com.github.games647.scoreboardstats.scoreboard.bukkit.BukkitSbManager;
import com.github.games647.scoreboardstats.scoreboard.protocol.PacketSbManager;
import com.github.games647.scoreboardstats.variables.ReplaceManager;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents the main class of this plugin.
 */
public class ScoreboardStats extends JavaPlugin {

    //don't create instances here that accesses the bukkit API - it will be incomptible with older mc versions
    private RefreshTask refreshTask;
    private Settings settings;
    private ReloadFixLoader classLoader;
    private SbManager scoreboardManager;
    private Database database;

    /**
     * Get the class loader for this plugin. This is a workaround to make
     * it available for other classes, because getClassLoader is as default
     * protected.
     *
     * @return the class loader for this plugin
     */
    public ReloadFixLoader getClassLoaderBypass() {
        return classLoader;
    }

    /**
     * Get the scoreboard manager.
     *
     * @return the manager
     */
    public SbManager getScoreboardManager() {
        return scoreboardManager;
    }

    /**
     * Get the replace manager.
     *
     * @return the manager
     */
    public ReplaceManager getReplaceManager() {
        if (scoreboardManager == null) {
            return null;
        }

        return scoreboardManager.getReplaceManager();
    }

    /**
     * Get the refresh task for updating the scoreboard
     *
     * @return the refresh task instance
     */
    public RefreshTask getRefreshTask() {
        return refreshTask;
    }

    /**
     * The database manager for pvp stats
     *
     * @return pvp stats database manager
     */
    public Database getStatsDatabase() {
        return database;
    }

    @Override
    public EbeanServer getDatabase() {
        if (database == null) {
            return super.getDatabase();
        }

        //this method exists to make it easier access from another plugin
        return database.getDatabaseInstance();
    }

    @Override
    public List<Class<?>> getDatabaseClasses() {
        final List<Class<?>> classes = Lists.newArrayList();
        classes.add(PlayerStats.class);
        return classes;
    }

    /**
     * Load the plugin.
     */
    @Override
    public void onLoad() {
        //Create a logger that is available by just the plugin name
        //have to be peformed before the first logging message by this plugin, so it prints it correctly
        Logger.getLogger(getName()).setParent(getLogger());

        //Check if server can display scoreboards; the version can only be with a complete shutdown
        checkScoreboardCompatibility();

        //this is needed by settings (for localized messages)
        classLoader = new ReloadFixLoader(this, getClassLoader());
    }

    /**
     * Enable the plugin
     */
    @Override
    public void onEnable() {
        if (!this.isEnabled()) {
            //cancel initialization if the already disabled it
            return;
        }

        //Load the config + needs to be initialised to get the configurated value for update-checking
        settings = new Settings(this);
        settings.loadConfig();

        if (Settings.isUpdateEnabled()) {
            //start this as early as possible, so it can run async in the background
            new UpdaterFix(this, this.getFile(), true, new UpdateCallback() {

                @Override
                public void onFinish(Updater updater) {
                    //This method will be performed on the main thread after the
                    //update check finished so this won't block the main thread
                    if (updater.getResult() == UpdateResult.SUCCESS) {
                        getLogger().info(Lang.get("onUpdate"));
                    }
                }
            });
        }

        refreshTask = new RefreshTask(this);

        //Register all events
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        //register all commands based on the root command of this plugin
        getCommand(getName().toLowerCase()).setExecutor(new SidebarCommands(this));

        //start tracking the ticks
        getServer().getScheduler().runTaskTimer(this, new TicksPerSecondTask(), 5 * 20L, 3 * 20L);
        //Start the refresh task; it should run on every tick, because it's smoothly update the variables with limit
        getServer().getScheduler().runTaskTimer(this, refreshTask, 5 * 20L, 1L);

        if (Settings.isCompatibilityMode()) {
            scoreboardManager = new PacketSbManager(this);
        } else {
            scoreboardManager = new BukkitSbManager(this);
        }

        if (Settings.isPvpStats()) {
            database = new Database(this);
            database.setupDatabase();
        }

        //creates scoreboards for every player that is online
        scoreboardManager.registerAll();
    }

    /**
     * Disable the plugin
     */
    @Override
    public void onDisable() {
        if (scoreboardManager != null) {
            //Clear all scoreboards
            scoreboardManager.unregisterAll();
        }

        if (database != null) {
            //flush the cache to the database
            database.saveAll();
        }

        if (getServer().getPluginManager().isPluginEnabled("ProtcolLib")) {
            //the plugin should disable all listeners including protocollibs
            ProtocolLibrary.getProtocolManager().removePacketListeners(this);
        }
    }

    /**
     * Reload the plugin
     */
    public void onReload() {
        if (settings != null) {
            settings.loadConfig();
        }

        if (refreshTask != null) {
            refreshTask.clear();
        }

        if (scoreboardManager != null) {
            scoreboardManager.unregisterAll();
        }

        if (Settings.isCompatibilityMode()) {
            scoreboardManager = new PacketSbManager(this);
        } else {
            scoreboardManager = new BukkitSbManager(this);
        }

        if (database != null) {
            database.setupDatabase();
        }

        scoreboardManager.registerAll();
    }

    private void checkScoreboardCompatibility() {
        //Scoreboards are introduced in minecraft 1.5
        final int compare = Version.compare("1.5", Version.getMinecraftVersionString());
        if (compare >= 0) {
            //The minecraft version is higher or equal the minimum scoreboard version
            return;
        }

        getLogger().warning(Lang.get("noCompatibleVersion"));
        //This plugin isn't compatible with the server version so we disabling it
        getPluginLoader().disablePlugin(this);
    }
}
