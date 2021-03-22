package me.cg360.games.tabletop.ngapimicro;

import net.cg360.nsapi.commons.data.Settings;
import net.cg360.nsapi.commons.data.keyvalue.Key;
import net.cg360.nsapi.commons.id.Identifier;

public class MicroGameProfile<T extends MicroGameBehaviour> {

    protected Identifier identifier;
    protected Class<T> behaviourClass;

    protected Settings properties;


    public MicroGameProfile(Identifier identifier, Class<T> behaviourClass, Settings properties) {
        this.identifier = identifier;
        this.behaviourClass = behaviourClass;

        this.properties = properties.lock();
    }



    public MicroGameWatchdog<T> createInstance(Settings settings) {
        try {
            T inst = behaviourClass.newInstance();
            MicroGameWatchdog<T> watchdog = new MicroGameWatchdog<>(inst);

            inst.setWatchdog(watchdog);
            inst.init(settings.lock());
            return watchdog;

        } catch (InstantiationException | IllegalAccessException err) {
            err.printStackTrace();
            return null;
        }
    }


    /** @return a typed key for this game profile. */
    public final Key<MicroGameProfile<T>> getKey() {
        return new Key<>(identifier);
    }

    public Identifier getIdentifier() { return identifier; }
    public Class<T> getBehaviours() { return behaviourClass; }
    public Settings getProperties() { return properties.getUnlockedCopy(); }
}
