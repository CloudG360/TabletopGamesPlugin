package me.cg360.games.tabletop.game;

import net.cg360.nsapi.commons.data.Settings;

public class MicroGameProfile<T extends MicroGameBehaviour> {

    protected Settings properties;
    protected Class<T> behaviourClass;


    public MicroGameProfile(Class<T> behaviourClass, Settings properties) {
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

    public Class<T> getBehaviours() { return behaviourClass; }
    public Settings getProperties() { return properties.getUnlockedCopy(); }
}
