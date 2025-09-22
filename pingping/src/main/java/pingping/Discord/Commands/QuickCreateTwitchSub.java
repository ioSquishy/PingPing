package pingping.Discord.Commands;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import javax.annotation.Nullable;

import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;
import org.tinylog.Logger;

import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.DiscordAPI;
import pingping.Discord.Helpers.PermissionlessRole;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.DiscordApiException;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Exceptions.TwitchApiException;

/**
 * Server specific Discord command for Indie VTuber Fan Server
 */
public class QuickCreateTwitchSub extends DiscordCommand {
    public static final String commandName = "quickcreatetwitchsub";
    private final static Map<Long, Long> registeredQuickCreations = new HashMap<Long, Long>(); // <server_id, pingchannel_id>
    static {
        DiscordCommandFactory.registerCommand(commandName, QuickCreateTwitchSub::new);

        // initialize registeredQuickCreations
        registeredQuickCreations.put(791040843279630356L, 842061663389614220L); // Test Server, testing-area
    }
    public QuickCreateTwitchSub(SlashCommandInteraction interaction) {
        super(commandName, interaction);
    }
    private static final String color_option_command_name = "color";
    @Override
    protected Optional<SlashCommandBuilder> getGlobalCommandBuilder() {
        SlashCommandBuilder slashCommandBuilder = new SlashCommandBuilder()
            .setName(commandName)
            .setDescription("Custom quick create command for Indite VTuber Fan Server.")
            .setDefaultEnabledForPermissions(PermissionType.ADMINISTRATOR)
            .setEnabledInDms(false)
            .addOption(new SlashCommandOptionBuilder()
                .setName(TwitchSub.Columns.BROADCASTER_ID.dcmd_argument_name)
                .setDescription("Streamer to register notification for.")
                .setType(SlashCommandOptionType.STRING)
                .setRequired(true)
                .build())
            .addOption(new SlashCommandOptionBuilder()
                .setName(color_option_command_name)
                .setDescription("Hex color code for the roles.")
                .setType(SlashCommandOptionType.STRING)
                .setRequired(false)
                .build());

        registeredQuickCreations.keySet().stream().forEach(server_id -> {
            try {
                slashCommandBuilder.createForServer(DiscordAPI.getAPI().getServerById(server_id).orElseThrow());
                Logger.trace("Registered command {} for server id {}", commandName, server_id);
            } catch (NoSuchElementException e) {
                Logger.error(e, "Failed to register {} command in server {}", commandName, server_id);
            }
        });

        Logger.trace("Registered command {} for all QuickCreate registered servers.");

        return Optional.empty();
    }
    @Override
    public void runCommand() {
        InteractionImmediateResponseBuilder response = interaction.createImmediateResponder();
        try {
            Logger.trace("{} discord command ran.", commandName);
            long serverId = this.interaction.getServer().get().getId();
            String streamer = this.interaction.getArgumentStringValueByName(TwitchSub.Columns.BROADCASTER_ID.dcmd_argument_name).get();
            Optional<String> hexColor = this.interaction.getArgumentStringValueByName(color_option_command_name);

            Color color = null;
            if (hexColor.isPresent()) {
                String rawHexColor = hexColor.get();
                if (!rawHexColor.startsWith("#")) {
                    rawHexColor = "#" + rawHexColor;
                }
                color = Color.decode(rawHexColor);
            }

            runCommand(serverId, streamer, color);
            response.setContent("Subscription added for: " + streamer).respond();
        } catch (NumberFormatException e) {
            Logger.debug(e, "Invalid hex color inputted.");
            response.setContent("Invalid hex color inputted.").respond();
        } catch (NoSuchElementException e) {
            Logger.error(e, "Discord command argument missing for command: {}", commandName);
            response.setContent("Command failed; Missing an argument.").respond();
        } catch (InvalidArgumentException  e) {
            Logger.debug(e);
            response.setContent(e.getMessage()).respond();
        } catch (DatabaseException | TwitchApiException | DiscordApiException e) {
            Logger.error(e);
            response.setContent(e.getMessage()).respond();
        }  catch (Exception e) {
            Logger.error(e, "Unforeseen exception in command: {}", commandName);
            response.setContent("Command failed for unforeseen reason...").respond();
        }
    }

    public static void runCommand(long server_id, String streamer, @Nullable Color role_color) throws InvalidArgumentException, TwitchApiException, DatabaseException, DiscordApiException {
        Logger.trace("{} command ran with arguments: server_id={}, streamer={}, role_color={}", commandName, server_id, streamer, role_color);

        // retrieve pre-set pingchannel for server
        Long pingchannel_id = registeredQuickCreations.get(server_id);
        if (pingchannel_id == null) {
            Logger.error("{} ran for an unregistered server with id: {}", commandName, server_id);
            throw new InvalidArgumentException("This server is not registered to run this command.");
        }

        runCommand(server_id, pingchannel_id, streamer, role_color);
        Logger.debug("Quick-created streamer subscription for streamer {} in server {}", streamer, server_id);
    }

    public static void runCommand(long server_id, long pingchannel_id, String streamer, @Nullable Color role_color) throws InvalidArgumentException, TwitchApiException, DatabaseException, DiscordApiException {
        Server server;
        try {
            server = DiscordAPI.getAPI().getServerById(server_id).orElseThrow();
        } catch (NoSuchElementException e) {
            Logger.error(e, "Command failed due to invalid server id being passed.");
            throw new InvalidArgumentException("Couldn't retrieve this server from Discord API for some reason...");
        }

        // quick create roles
        Role displayRole;
        Role pingRole;
        try {
            displayRole = PermissionlessRole.create(server, streamer, role_color);
            Logger.trace("Display role created with id: {}", displayRole.getId());

            pingRole = PermissionlessRole.create(server, streamer + " Ping", role_color);
            Logger.trace("Ping role created with id: {}", pingRole.getId());
        } catch (CompletionException e) {
            Logger.debug(e, "Failed to create roles for streamer.");
            throw new DiscordApiException("Failed to create roles for streamer. Does the bot have permissions?");
        }

        // register streamer using new pingrole
        try {
            RegisterTwitchSub.registerSub(server_id, streamer, pingRole.getId(), pingchannel_id);
        } catch (InvalidArgumentException | TwitchApiException | DatabaseException e) {
            Logger.error("RegisterTwitchSub failed, deleting created streamer roles.");
            try {
                displayRole.delete("Stream registration failed.").join();
                pingRole.delete("Stream registration failed.").join();
            } catch (CompletionException e1) {
                Logger.error(e1, "Failed to delete roles created for streamer after registration failed.");
            }
            throw e;
        }
    }
}
