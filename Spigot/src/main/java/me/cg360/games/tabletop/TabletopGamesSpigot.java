package me.cg360.games.tabletop;

import me.cg360.games.tabletop.command.CommandTableGame;
import me.cg360.games.tabletop.game.jenga.GBehaveJenga;
import me.cg360.games.tabletop.ngapimicro.MicroGameProfile;
import me.cg360.games.tabletop.ngapimicro.MicroGameRegistry;
import me.cg360.games.tabletop.ngapimicro.keychain.GamePropertyKeys;
import net.cg360.nsapi.commons.Check;
import net.cg360.nsapi.commons.data.Settings;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class TabletopGamesSpigot extends JavaPlugin {

    private static TabletopGamesSpigot tabletopGamesSpigot = null;

    private MicroGameRegistry microGameRegistry;

    private YamlConfiguration configurationFile;
    private Settings configuration;

    @Override
    public void onEnable() {

        try {
            tabletopGamesSpigot = this;

            loadConfiguration();

            // -- Set Managers --

            this.microGameRegistry = new MicroGameRegistry();
            this.microGameRegistry.setAsPrimaryRegistry();


            // -- Register Games --

            /*
            this.microGameRegistry.register(new MicroGameProfile<>(
                    Util.NAME.id("jenga"), GBehaveJenga.class,
                    new Settings()
                            .set(GamePropertyKeys.DISPLAY_NAME, "Jenga")
                            .set(GamePropertyKeys.DESCRIPTION, "Try to remove blocks from a tall tower without it toppling on your turn!")
                            .set(GamePropertyKeys.AUTHORS, "CG360")
            ));
            */


            // -- Register Commands --

            PluginCommand cmdTableGame = this.getServer().getPluginCommand(CommandTableGame.COMMAND);
            if (cmdTableGame != null) {
                cmdTableGame.setExecutor(new CommandTableGame());
                cmdTableGame.setDescription(CommandTableGame.DESCRIPTION);
                cmdTableGame.setUsage(CommandTableGame.USAGE);
            }

        } catch (Exception err){
            tabletopGamesSpigot = null;
            err.printStackTrace();
            // Just making sure everything is properly nulled.
        }
    }


    private void loadConfiguration() {
        this.configuration = new Settings();
        File cfgFile = new File(getDataFolder(), "config.yml");

        if(!cfgFile.exists()){
            cfgFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        YamlConfiguration config = new YamlConfiguration();

        try {
            config.load(cfgFile);
            this.configurationFile = config;

            // Set config to file's settings
            //this.configuration
            //        .set(ConfigKeys.KEY_HERE, config.getBoolean(ConfigKeys.KEY_HERE.get(), DEFAULT_VALUE))
            //;

        } catch (IOException | InvalidConfigurationException e){
            e.printStackTrace();
            this.configurationFile = null;
        }


    }

    public static TabletopGamesSpigot get() { return tabletopGamesSpigot; }
    public static Logger getLog() { return get().getLogger(); }
    public static BukkitScheduler getScheduler() { return get().getServer().getScheduler(); }
    public static boolean isRunning() { return tabletopGamesSpigot != null; }

    public static MicroGameRegistry getMicroGameRegistry() { return get().microGameRegistry; }

    public static Settings getConfiguration() { return get().configuration; }
    public static YamlConfiguration getConfigurationFile() { return get().configurationFile; }
}
