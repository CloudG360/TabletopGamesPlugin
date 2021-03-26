package me.cg360.games.tabletop;

import cn.nukkit.entity.Entity;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLogger;
import cn.nukkit.scheduler.ServerScheduler;
import cn.nukkit.utils.Config;
import me.cg360.games.tabletop.command.CommandTableGame;
import me.cg360.games.tabletop.game.jenga.GBehaveJenga;
import me.cg360.games.tabletop.game.jenga.entity.EntityJengaBlockCollider;
import me.cg360.games.tabletop.game.jenga.entity.EntityVisualJengaBlock;
import me.cg360.games.tabletop.ngapimicro.MicroGameProfile;
import me.cg360.games.tabletop.ngapimicro.MicroGameRegistry;
import me.cg360.games.tabletop.ngapimicro.keychain.GamePropertyKeys;
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


            // -- Register Games --

            this.microGameRegistry.register(new MicroGameProfile<>(
                    Util.NAME.id("jenga"), GBehaveJenga.class,
                    new Settings()
                            .set(GamePropertyKeys.DISPLAY_NAME, "Jenga")
                            .set(GamePropertyKeys.DESCRIPTION, "Try to remove blocks from a tall tower without it toppling on your turn!")
                            .set(GamePropertyKeys.AUTHORS, "CG360")
            ));


            // -- Register Entities --

            Entity.registerEntity(EntityVisualJengaBlock.class.getSimpleName(), EntityVisualJengaBlock.class);
            Entity.registerEntity(EntityJengaBlockCollider.class.getSimpleName(), EntityJengaBlockCollider.class);


            // -- Register Commands --

            this.getServer().getCommandMap().register("ngapi", new CommandTableGame());


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
    public static ServerScheduler getScheduler() { return get().getServer().getScheduler(); }
    public static boolean isRunning() { return tabletopGamesNukkit != null; }

    public static MicroGameRegistry getMicroGameRegistry() { return get().microGameRegistry; }

    public static Settings getConfiguration() { return get().configuration; }
    public static Config getConfigurationFile() { return get().configurationFile; }
}
