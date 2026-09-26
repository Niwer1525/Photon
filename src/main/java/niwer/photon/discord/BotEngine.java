package niwer.photon.discord;

import java.time.Duration;
import java.time.OffsetDateTime;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import niwer.lumen.Console;
import niwer.photon.Directories;
import niwer.photon.PhotonEngine;
import niwer.photon.discord.commands.CommandsManager;
import niwer.photon.sql.DiscordLogTable;
import niwer.photon.sql.DiscordLogTable.ModerationType;
import niwer.photon.util.PhotonLogTypes;

/**
 * Main class of the bot, load the bot and register important slash commands
 */
@SuppressWarnings("null") // The compiler in Photon is not good at handling JDA's @Nonnull annotations, so we suppress null warnings in this class
public class BotEngine extends ListenerAdapter {

    private static JDA bot;
    private static boolean isRestarting = false;

    public static Guild guild;

    /**
     * Load the bot, register slash commands and start it
     * 
     * @throws LoginException
     * @throws InterruptedException
     */
    public static void load(boolean shouldRestart) {
        Thread t = new Thread(() -> {
            if(!Directories.getConfig().hasBotToken()) throw new IllegalStateException("Discord bot token is not set. Please check the configuration file.");

            try {
                /* Try to silence JDA logs */
                PhotonLogTypes.silenceLogsFor("net.dv8tion.jda");
                
                final JDABuilder BUILDER = JDABuilder.createDefault(Directories.getConfig().discord_bot_token);
                BUILDER.setActivity(Activity.playing(Directories.getConfig().bot_activity));
                BUILDER.addEventListeners(new BotEngine());
                BUILDER.addEventListeners(new CommandsManager());
                BUILDER.enableIntents(GatewayIntent.MESSAGE_CONTENT);
                BUILDER.enableIntents(GatewayIntent.GUILD_MEMBERS);
                BUILDER.enableIntents(GatewayIntent.GUILD_PRESENCES);
                BUILDER.enableIntents(GatewayIntent.GUILD_MODERATION);
                BUILDER.setMemberCachePolicy(MemberCachePolicy.ALL);
                bot = BUILDER.build();

                isRestarting = shouldRestart;

                CommandsManager.load();
                Thread.sleep(550); // Wait a bit to ensure guild is loaded

                /* Log that the Discord Bot is running */
                Console.log("Discord Bot started successfully").type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
            } catch (InterruptedException e) {
                Console.log("Failed to start Discord Bot: " + e.getMessage()).type(PhotonLogTypes.NETWORK).error().container(PhotonEngine.LOGGER).send();
                e.printStackTrace();
            }
        }, "Discord Bot Thread");
        t.start();
    }

    /**
     * When the bot is ready, register global slash commands
     */
    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        event.getJDA().updateCommands().addCommands(CommandsManager.getGlobalCommands()).queue();
    }

    /**
     * Register slash commands when the bot is ready
     * 
     * @param event The event of the bot being ready
     * @author Mini
     */
    @Override
    public void onGuildReady(@Nonnull GuildReadyEvent event) {
        guild = event.getGuild();
        guild.updateCommands().addCommands(CommandsManager.getGuildCommands()).queue();
        if (isRestarting) {
            guild.getTextChannelById(Directories.getConfig().network_console_channel_id).sendMessage("Network restarted").queue();
            isRestarting = false;
        }
    }

    /**
     * Log a moderation action (ban, unban, kick, timeout) in the database without duration (for bans, unbans and kicks)
     * @param guild The guild where the action took place
     * @param targetId The ID of the user targeted by the action
     * @param type The type of the moderation action
     */
    private static void logModerationAction(@Nonnull Guild guild, String targetId, ModerationType type) {
        logModerationAction(guild, targetId, type, 0L);
    }

    /**
     * Log a moderation action (ban, unban, kick, timeout) in the database
     * @param guild The guild where the action took place
     * @param targetId The ID of the user targeted by the action
     * @param type The type of the moderation action
     * @param durationSeconds The duration of the action in seconds (for timeouts, 0 for other actions)
     */
    private static void logModerationAction(@Nonnull Guild guild, String targetId, ModerationType type, long durationSeconds) {
        guild.retrieveAuditLogs().type(type.toDiscordActionType()).limit(1).queue(entries -> {
            if(entries.isEmpty()) return; // No entry, No log

            final AuditLogEntry AUDIT = entries.stream().filter(entry -> entry.getTargetId().equals(targetId)).findFirst().orElse(null);
            if (AUDIT == null) return; // No relevant entry, No log
            
            final String MODERATOR_ID = AUDIT.getUser() != null ? AUDIT.getUser().getId() : null;
            final String REASON = AUDIT.getReason();
            DiscordLogTable.save(guild.getId(), targetId, type, REASON, MODERATOR_ID, durationSeconds);
        });
    }

    @Override
    public void onGuildBan(@Nonnull GuildBanEvent event) {
        final String TARGET_USER_ID = event.getUser().getId();
        logModerationAction(event.getGuild(), TARGET_USER_ID, ModerationType.BAN);
    }

    @Override
    public void onGuildUnban(@Nonnull GuildUnbanEvent event) {
        final String TARGET_USER_ID = event.getUser().getId();
        logModerationAction(event.getGuild(), TARGET_USER_ID, ModerationType.UNBAN); // No reasons
    }

    @Override
    public void onGuildMemberUpdateTimeOut(@Nonnull GuildMemberUpdateTimeOutEvent event) {
        final OffsetDateTime TIMEOUT_END = event.getNewTimeOutEnd();
        if (TIMEOUT_END == null) return; // If the timeout end is null, it means the user has been removed from timeout, we log it as an untimeout with duration 0

        final long DURATION_SECONDS = Duration.between(OffsetDateTime.now(), TIMEOUT_END).getSeconds();
        final String TARGET_USER_ID = event.getUser().getId();
        logModerationAction(event.getGuild(), TARGET_USER_ID, ModerationType.TIMEOUT, DURATION_SECONDS);
    }
    
    /**
     * To log something on discord console manager channel
     * 
     * @param data The data to log, if the console channel is configured and the bot is in the official guild, it will be sent to the console channel, otherwise it will be sent to the network logs channel
     */
    public static void log(Console data) {
        if(!hasConsoleChannel() && !isOfficialGuild(guild)) return;

        final EmbedBuilder LOGS_EMBED = new EmbedBuilder();
        LOGS_EMBED.setColor(data.type().color().color());
        LOGS_EMBED.setTitle((data.isError() ? "[Error] " : "") + data.type().name() + (Directories.getConfig().isDevEnvironment() ? " [DEV]" : ""));
        LOGS_EMBED.setDescription(String.format("%s", data.message()));
        LOGS_EMBED.setTimestamp(OffsetDateTime.now());
        getConsoleChannel().sendMessageEmbeds(LOGS_EMBED.build()).queue();

        if (data.file() != null) {
            final FileUpload FILE_TO_UPLOAD = FileUpload.fromData(data.file(), "logs.txt");
            getConsoleChannel().sendFiles(FILE_TO_UPLOAD).queue();
        }
    }

    public static boolean isOfficialGuild(Guild guildToCheck) {
        if(guildToCheck == null) return false;
        return guildToCheck.getId().equals(Directories.getConfig().official_discord_server_id);
    }

    public static boolean hasConsoleChannel() { return getConsoleChannel() != null; }

    public static boolean isConsoleChannel(GuildChannel channel) {
        if(channel == null) return false;
        if(!hasConsoleChannel()) return false;
        return channel.getId().equals(getConsoleChannel().getId());
    }

    /**
     * @return The console channel where the bot/network logs arrive
     */
    public static StandardGuildMessageChannel getConsoleChannel() {
        if(guild == null) return null;
        if(Directories.getConfig().network_console_channel_id == null || Directories.getConfig().network_console_channel_id.isEmpty()) return null;
        return guild.getTextChannelById(Directories.getConfig().network_console_channel_id);
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        handleMessages(event.getGuildChannel(), event.getMessage());
    }

    /**
     * When a message is updated
     * 
     * @param event The event of a message being update
     */
    @Override
    public void onMessageUpdate(@Nonnull MessageUpdateEvent event) {
        handleMessages(event.getGuildChannel(), event.getMessage());
    }

    private static void handleMessages(GuildChannel eventChannel, Message message) {
        final User AUTHOR = message.getAuthor();
        if (AUTHOR.isBot()) return;

        if(isConsoleChannel(eventChannel)) {
            message.delete().queue(); // Delete the edited message
            return;
        }
    }

    public static boolean isBotInitialized() { return bot != null; }
}