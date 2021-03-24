package me.cg360.games.tabletop.ngapimicro;

import me.cg360.games.tabletop.TabletopGamesNukkit;
import net.cg360.nsapi.commons.Check;
import net.cg360.nsapi.commons.data.Settings;
import net.cg360.nsapi.commons.data.keyvalue.Key;
import net.cg360.nsapi.commons.id.Identifier;

public class MicroGameProfile<T extends MicroGameBehaviour> {

    protected Identifier identifier;
    protected Class<T> behaviourClass;

    protected Settings properties;


    public MicroGameProfile(Identifier identifier, Class<T> behaviourClass, Settings properties) {
        Check.nullParam(identifier, "identifier");
        Check.nullParam(behaviourClass, "behaviourClass");

        this.identifier = identifier;
        this.behaviourClass = behaviourClass;

        this.properties = (properties == null ? new Settings() : properties).lock();
    }



    public MicroGameWatchdog<T> createInstance(Settings settings) {
        try {
            T inst = behaviourClass.newInstance();
            MicroGameWatchdog<T> watchdog = new MicroGameWatchdog<>(this, inst);

            try {
                inst.setWatchdog(watchdog);
                inst.init(settings.lock());
                watchdog.initRules();

            } catch (Exception err) {
                err.printStackTrace();
                TabletopGamesNukkit.getLog().error(String.format("Error whilst running the game '%s', stopping game.", identifier.getID()));
                watchdog.stopGame();
            }
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
