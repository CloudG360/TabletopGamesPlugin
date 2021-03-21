package me.cg360.games.tabletop;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLogger;
import cn.nukkit.utils.Config;
import me.cg360.games.tabletop.game.MicroGameRegistry;
import net.cg360.nsapi.commons.data.Settings;

import java.io.File;

public class TabletopGamesNukkit extends PluginBase {

    private static TabletopGamesNukkit tabletopGamesNukkit = null;

    private MicroGameRegistry microGameRegistry;

    private Config configurationFile;
    private Settings configuration;

    @Override
    public void onEnable() {

        try {
            tabletopGamesNukkit = this;

            loadConfiguration();

            // -- Set Managers --

            this.microGameRegistry = new MicroGameRegistry();

            this.microGameRegistry.setAsPrimaryRegistry();

            // -- Register listeners --


            // -- Register Commands --

        } catch (Exception err){
            tabletopGamesNukkit = null;
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
        this.configurationFile = new Config(new File(this.getDataFolder(), "config.yml"), Config.YAML);

        //this.configuration
        //        .set(ConfigKeys.KEY_HERE, config.getBoolean(ConfigKeys.KEY_HERE.get(), DEFAULT_VALUE))
        //;
    }

    public static TabletopGamesNukkit get() { return tabletopGamesNukkit; }
    public static PluginLogger getLog() { return get().getLogger(); }
    public static boolean isRunning() { return tabletopGamesNukkit != null; }

    public static MicroGameRegistry getMicroGameRegistry() { return get().microGameRegistry; }

    public static Settings getConfiguration() { return get().configuration; }
    public static Config getConfigurationFile() { return get().configurationFile; }
}
