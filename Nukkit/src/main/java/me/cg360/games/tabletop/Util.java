package me.cg360.games.tabletop;

import net.cg360.nsapi.commons.data.keyvalue.Key;

public class Util {

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
}
