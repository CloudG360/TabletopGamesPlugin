package me.cg360.games.tabletop.ngapimicro;

import me.cg360.games.tabletop.TabletopGamesSpigot;
import net.cg360.nsapi.commons.data.keyvalue.Key;
import net.cg360.nsapi.commons.id.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class MicroGameRegistry {

    private static MicroGameRegistry primaryRegistry;

    private HashMap<String, MicroGameProfile<?>> gameProfiles;


    public MicroGameRegistry() {
        this.gameProfiles = new HashMap<>();
    }

    /**
     * Sets the registry of the result provided from MicroGameRegistry#get() and
     * finalizes the instance to an extent.
     *
     * Cannot be changed once initially called.
     */
    public void setAsPrimaryRegistry(){
        if(primaryRegistry == null) primaryRegistry = this;
    }



    /** @return the profile's key for use in key lists. Is silent if a duplicate occurs. */
    public <T extends MicroGameBehaviour> Key<MicroGameProfile<T>> registerProfile(MicroGameProfile<T> profile) {
        register(profile);
        return profile.getKey();
    }

    /** @return true if there was not a profile already registered. */
    public boolean register(MicroGameProfile<?> profile) {
        String key = profile.getKey().get();

        if(!gameProfiles.containsKey(key)) {
            this.gameProfiles.put(key, profile);
            return true;
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    public <T extends MicroGameProfile<?>> Optional<T> getProfile(Key<T> key) {
        String k = key.get();

        if(gameProfiles.containsKey(k)) {
            try {
                T profile = (T) gameProfiles.get(k);
                return Optional.of(profile);
            } catch (ClassCastException err) {
                TabletopGamesSpigot.getLog().warning("Plugin tried getting a specific MicroGameProfile with the key '%s' however it was the wrong type.");
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public Optional<MicroGameProfile<?>> getProfile(Identifier identifier) {
        String k = identifier.getID();

        if(gameProfiles.containsKey(k)) {
            return Optional.of(gameProfiles.get(k));
        }
        return Optional.empty();
    }



    public ArrayList<MicroGameProfile<?>> getGameProfiles() {
        return new ArrayList<>(gameProfiles.values());
    }



    /** @return the primary version of this registry. */
    public static MicroGameRegistry get() {
        return primaryRegistry;
    }
}
