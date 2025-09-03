package pingping.Discord.Commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.javacord.api.interaction.SlashCommandInteraction;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.tinylog.Logger;

public class DiscordCommandFactory {
    private static final Map<String, Function<SlashCommandInteraction, DiscordCommand>> registeredCommands = new HashMap<String, Function<SlashCommandInteraction, DiscordCommand>>();

    protected static void registerCommand(String commandName, Function<SlashCommandInteraction, DiscordCommand> command) {
        Function<SlashCommandInteraction, DiscordCommand> previousRegisteredCommand = registeredCommands.putIfAbsent(commandName, command);
        if (previousRegisteredCommand != null) {
            throw new IllegalArgumentException("Attempted to register a command with duplicate command name: " + commandName);
        }
    }

    public DiscordCommand createCommand(String commandName, SlashCommandInteraction commandInteraction) {
        Function<SlashCommandInteraction, DiscordCommand> constructor = registeredCommands.get(commandName);
        if (constructor == null) {
            throw new IllegalArgumentException("Unknown command name: " + commandName);
        }
        return constructor.apply(commandInteraction);
    }

    /**
     * Forces subclasses of DiscordCommand to register themselves with this factory.
     * Needs to be ran before running subclass commands.
     */
    public static void forceLoadCommandClasses() {
        final String packageName = "pingping.Discord.Commands";
        ConfigurationBuilder config = new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage(packageName))
            .filterInputsBy(new FilterBuilder().includePackage(packageName))
            .setScanners(Scanners.SubTypes);
        
        Reflections reflections = new Reflections(config);
        Set<Class<? extends DiscordCommand>> subTypes = reflections.getSubTypesOf(DiscordCommand.class);
        subTypes.forEach(type -> {
            try {
                Class.forName(type.getName());
                Logger.info("Registered DiscordCommand: {}", type.getName());
            } catch (ClassNotFoundException e) {
                // should be impossible
                Logger.error(e);
            }
        });
    }
}
