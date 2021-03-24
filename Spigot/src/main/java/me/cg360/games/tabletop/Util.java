package me.cg360.games.tabletop;

import net.cg360.nsapi.commons.data.keyvalue.Key;
import net.cg360.nsapi.commons.id.Namespace;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Util {

    public static final ChatColor DEFAULT_TEXT_COLOUR = ChatColor.GRAY;

    public static final Namespace NAME = new Namespace("tabletop");

    public static final String BASE_PERMISSION = "tabletop";
    public static final String COMMAND_PERMISSION = BASE_PERMISSION+".command";

    /**
     * Shorthand method for checking OofTracker's configuration booleans.
     * @param boolKey the key in the config
     * @param orElse the fallback value
     * @return the value stored, else the fallback value.
     */
    public static boolean check(Key<Boolean> boolKey, boolean orElse) {
        return TabletopGamesSpigot.getConfiguration().getOrElse(boolKey, orElse);
    }

    public static <T extends Enum<T>> T stringToEnum(Class<T> enumBase, String value, T def) {
        if(value == null) {
            return def;
        } else {
            try {
                return Enum.valueOf(enumBase, value.toUpperCase());
            } catch (Exception err) {
                TabletopGamesSpigot.getLog().warning(String.format("%s is not a valid enum!", value));
                return def;
            }
        }
    }

    public static boolean permissionCheck(CommandSender subject, String permission) {

        if(!subject.hasPermission(permission)) {
            subject.sendMessage(eMessage("You don't have the correct permissions to perform this action."));
            return false;
        }
        return true;
    }

    public static String eMessage(String text){
        return fMessage("ERROR", ChatColor.DARK_RED, text, ChatColor.RED);
    }

    public static String fMessage(String topic, ChatColor topicColour, String text){
        return fMessage(topic, topicColour, text, DEFAULT_TEXT_COLOUR);
    }

    public static String fMessage(String topic, ChatColor topicColour, String text, ChatColor defaultTextColour){
        return String.format("%s%s%s %s%s>> %s%s%s", topicColour, ChatColor.BOLD, topic.toUpperCase(), ChatColor.DARK_GRAY, ChatColor.BOLD, ChatColor.RESET, defaultTextColour, text);
    }
}
