package me.cg360.games.tabletop;

import cn.nukkit.utils.TextFormat;
import net.cg360.nsapi.commons.data.keyvalue.Key;

public class Util {

    public static final TextFormat DEFAULT_TEXT_COLOUR = TextFormat.GRAY;

    /**
     * Shorthand method for checking OofTracker's configuration booleans.
     * @param boolKey the key in the config
     * @param orElse the fallback value
     * @return the value stored, else the fallback value.
     */
    public static boolean check(Key<Boolean> boolKey, boolean orElse) {
        return TabletopGamesNukkit.getConfiguration().getOrElse(boolKey, orElse);
    }

    public static <T extends Enum<T>> T stringToEnum(Class<T> enumBase, String value, T def) {
        if(value == null) {
            return def;
        } else {
            try {
                return Enum.valueOf(enumBase, value.toUpperCase());
            } catch (Exception err) {
                TabletopGamesNukkit.getLog().warning(String.format("%s is not a valid enum!", value));
                return def;
            }
        }
    }

    public static String eMessage(String text){
        return fMessage("ERROR", TextFormat.DARK_RED, text, TextFormat.RED);
    }

    public static String fMessage(String topic, TextFormat topicColour, String text){
        return fMessage(topic, topicColour, text, DEFAULT_TEXT_COLOUR);
    }

    public static String fMessage(String topic, TextFormat topicColour, String text, TextFormat defaultTextColour){
        return String.format("%s%s%s %s%s>> %s%s%s", topicColour, TextFormat.BOLD, topic.toUpperCase(), TextFormat.DARK_GRAY, TextFormat.BOLD, TextFormat.RESET, defaultTextColour, text);
    }
}
