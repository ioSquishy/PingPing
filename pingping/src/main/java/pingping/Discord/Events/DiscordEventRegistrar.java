package pingping.Discord.Events;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.tinylog.Logger;

public class DiscordEventRegistrar {
    private static final Map<String, Supplier<DiscordEvent>> registeredEvents = new HashMap<String, Supplier<DiscordEvent>>();

    protected static void registerEvent(String eventName, Supplier<DiscordEvent> eventClassSupplier) {
        Supplier<DiscordEvent> previousRegisteredEventClassSupplier = registeredEvents.putIfAbsent(eventName.strip(), eventClassSupplier);
        if (previousRegisteredEventClassSupplier != null) {
            throw new IllegalArgumentException("Attempted to register an event class with duplicate name: " + eventName);
        } else {
            Logger.trace("Registered DiscordEvent: {}", eventName);
        }
    }

    /**
     * Forces subclasses of DiscordEvent to register themselves with this registrar.
     * Needs to be ran before running registerEvents().
     */
    public static void forceLoadEventClasses() {
        Logger.trace("Force loading Discord event classes...");
        final String packageName = "pingping.Discord.Events";
        ConfigurationBuilder config = new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage(packageName))
            .filterInputsBy(new FilterBuilder().includePackage(packageName))
            .setScanners(Scanners.SubTypes);
        
        Reflections reflections = new Reflections(config);
        Set<Class<? extends DiscordEvent>> subTypes = reflections.getSubTypesOf(DiscordEvent.class);
        subTypes.forEach(type -> {
            try {
                Class.forName(type.getName());
                Logger.trace("Loaded DiscordEvent: {}", type.getSimpleName());
            } catch (ClassNotFoundException e) {
                // should be impossible
                Logger.error(e);
            }
        });

        Logger.info("Discord Events registered.");
    }

    public static void registerEvents() {
        for (Supplier<DiscordEvent> eventClass : registeredEvents.values()) {
            eventClass.get().registerEventListener();
        }
    }
}
