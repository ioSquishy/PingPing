package pingping.Discord.Commands;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.tinylog.Logger;

import pingping.Discord.DiscordAPI;

public class DiscordCommandFactory {
    private static final Map<String, Function<SlashCommandInteraction, DiscordCommand>> registeredCommands = new HashMap<String, Function<SlashCommandInteraction, DiscordCommand>>();

    /**
     * 
     * @param commandName
     * @param command
     * @throws IllegalArgumentException if commandName is not all lowercase
     */
    protected static void registerCommand(String commandName, Function<SlashCommandInteraction, DiscordCommand> command) throws IllegalArgumentException {
        // if command name isn't all lower case throw error
        if (!commandName.toLowerCase().equals(commandName)) {
            throw new IllegalArgumentException("Command name must be all lowercase to conform with Discord command schema.");
        }

        Function<SlashCommandInteraction, DiscordCommand> previousRegisteredCommand = registeredCommands.putIfAbsent(commandName.strip(), command);
        if (previousRegisteredCommand != null) {
            throw new IllegalArgumentException("Attempted to register a command with duplicate command name: " + commandName);
        } else {
            Logger.trace("Registered DiscordCommand: {}", commandName);
        }
    }

    public static DiscordCommand createCommand(String commandName, SlashCommandInteraction commandInteraction) throws IllegalArgumentException {
        Function<SlashCommandInteraction, DiscordCommand> constructor = registeredCommands.get(commandName.strip());
        if (constructor == null) {
            throw new IllegalArgumentException("Unknown command name: " + commandName);
        }
        return constructor.apply(commandInteraction);
    }

    /**
     * Forces subclasses of DiscordCommand to register themselves with this factory.
     * Needs to be ran before running subclass commands from Discord.
     */
    public static void forceLoadCommandClasses() {
        Logger.trace("Force loading Discord command classes...");
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
                Logger.trace("Loaded DiscordCommand: {}", type.getSimpleName());
            } catch (ClassNotFoundException e) {
                // should be impossible
                Logger.error(e);
            }
        });

        Logger.info("Discord Commands registered.");
    }

    public static void registerGlobalCommandsInApi() {
        Logger.trace("Registering global Discord commands in API...");
        Set<SlashCommandBuilder> commandBuilders = new HashSet<SlashCommandBuilder>();
        for (Function<SlashCommandInteraction, DiscordCommand> commandFunction : registeredCommands.values()) {
            DiscordCommand discordCommand = commandFunction.apply(null);
            Optional<SlashCommandBuilder> discordCommandBuilder = discordCommand.getGlobalCommandBuilder();
            if (discordCommandBuilder.isPresent()) {
                commandBuilders.add(discordCommand.getGlobalCommandBuilder().get());
                Logger.trace("Added command {} to global application command overwrite call...", discordCommand.commandName);
            }
        }
        
        DiscordAPI.getAPI().bulkOverwriteGlobalApplicationCommands(commandBuilders).join();
    }
}
