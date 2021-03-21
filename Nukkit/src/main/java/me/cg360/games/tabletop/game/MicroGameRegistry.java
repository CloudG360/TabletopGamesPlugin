package me.cg360.games.tabletop.game;

import net.cg360.nsapi.commons.data.keyvalue.Key;

import java.util.HashMap;

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



    /** @return the primary version of this registry. */
    public static MicroGameRegistry get() {
        return primaryRegistry;
    }
}
