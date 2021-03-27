package me.cg360.games.tabletop.command;

import me.cg360.games.tabletop.TabletopGamesSpigot;
import me.cg360.games.tabletop.Util;
import me.cg360.games.tabletop.ngapimicro.MicroGameProfile;
import me.cg360.games.tabletop.ngapimicro.MicroGameRegistry;
import me.cg360.games.tabletop.ngapimicro.keychain.GamePropertyKeys;
import me.cg360.games.tabletop.ngapimicro.keychain.InitKeys;
import net.cg360.nsapi.commons.data.Settings;
import net.cg360.nsapi.commons.id.Identifier;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class CommandMicroGame implements CommandExecutor {

    public static final String COMMAND = "microgame";
    public static final String DESCRIPTION = "Provides tools to manage the micro-games registered in the Micro-Game registry.";
    public static final String USAGE = "/microgame start <String: Micro-Game ID>\nOR /microgame list\nOR/microgame detail <String: game_id>";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if(args.length < 1) {
            sender.sendMessage(Util.eMessage("This command requires at least 1 parameter. (sub-command)"));
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "start": {
                if (!Util.permissionCheck(sender, Util.COMMAND_PERMISSION + ".microgame.start")) return true;

                if (args.length < 2) {
                    sender.sendMessage(Util.eMessage("This sub-command requires 2 parameters. (Game ID)"));
                    return true;
                }
                Optional<MicroGameProfile<?>> p = MicroGameRegistry.get().getProfile(new Identifier(args[1]));

                if (p.isPresent()) {
                    MicroGameProfile<?> profile = p.get();
                    Settings gameSettings = new Settings();

                    if(sender instanceof Player) {
                        Player player = (Player) sender;
                        gameSettings.set(InitKeys.HOST_PLAYER, player);
                        gameSettings.set(InitKeys.ORIGIN, player.getLocation());
                    }

                    //TODO: Add a way to provide origin with parameters.

                    profile.createInstance(gameSettings);
                    sender.sendMessage(Util.fMessage("MICRO", ChatColor.DARK_AQUA, "Started the game!"));

                } else {
                    sender.sendMessage(Util.eMessage("This specified micro-game type does not exist."));
                }
            } break;



            case "list": {
                if (!Util.permissionCheck(sender, Util.COMMAND_PERMISSION + ".microgame.list")) return true;

                sender.sendMessage(Util.fMessage("MICRO", ChatColor.DARK_AQUA, "The currently registered games are:"));

                for (MicroGameProfile<?> profile : MicroGameRegistry.get().getGameProfiles()) {
                    sender.sendMessage(String.format("%s - %s%s", ChatColor.DARK_GRAY, ChatColor.AQUA, profile.getIdentifier().getID()));
                } // Maybe this should be async? oh well.
            } break;



            case "detail": {
                if (!Util.permissionCheck(sender, Util.COMMAND_PERMISSION + ".microgame.detail")) return true;

                if (args.length < 2) {
                    sender.sendMessage(Util.eMessage("This sub-command requires 2 parameters. (Game ID)"));
                    return true;
                }
                Optional<MicroGameProfile<?>> p = MicroGameRegistry.get().getProfile(new Identifier(args[1]));

                if (p.isPresent()) {
                    MicroGameProfile<?> profile = p.get();
                    sender.sendMessage(Util.fMessage("MICRO", ChatColor.DARK_AQUA, String.format("Listing details for type %s'%s'", ChatColor.AQUA, args[1].toLowerCase())));

                    String name = profile.getProperties().getOrElse(GamePropertyKeys.DISPLAY_NAME, "???");
                    String description = profile.getProperties().getOrElse(GamePropertyKeys.DESCRIPTION, "???");
                    String authors = profile.getProperties().getOrElse(GamePropertyKeys.AUTHORS, "???");

                    sender.sendMessage(String.format("%s%s - Name: %s%s%s", ChatColor.GRAY, ChatColor.BOLD, ChatColor.RESET, ChatColor.AQUA, name));
                    sender.sendMessage(String.format("%s%s - Description: %s%s%s", ChatColor.GRAY, ChatColor.BOLD, ChatColor.RESET, ChatColor.AQUA, description));
                    sender.sendMessage(String.format("%s%s - Authors: %s%s%s", ChatColor.GRAY, ChatColor.BOLD, ChatColor.RESET, ChatColor.AQUA, authors));

                } else {
                    sender.sendMessage(Util.eMessage("This specified micro-game type does not exist."));
                }
            } break;



            default:
                sender.sendMessage(Util.eMessage("Invalid command! (sub-command)"));
                sender.sendMessage(ChatColor.RED + USAGE);
                break;
        }

        return true;
    }
}
