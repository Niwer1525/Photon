package niwer.photon.discord.commands;

import java.util.function.Function;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import niwer.photon.discord.BotEngine;
import niwer.photon.discord.commands.AutoCompleteRegistry.AutoCompleteProvider;
import niwer.photon.util.TranslationManager;

/**
 * @author Niwer
 */
public abstract class AbstractSlashCommand {

    private final String CMD_NAME;
    protected final CommandData DATA;

    protected AbstractSlashCommand(String cmdName, String cmdDescription) {
        this.CMD_NAME = cmdName;
        this.DATA = Commands.slash(this.CMD_NAME, cmdDescription);
    }

    public final SlashCommandData data() { return (SlashCommandData) this.DATA; }

    /**
     * Add an option without autocomplete support
     * @param type The option type
     * @param name The option name
     * @param description The option description
     * @param isRequired Whether the option is required
     * @return The SlashCommandData for chaining
     */
    protected final SlashCommandData addOption(OptionType type, String name, String description, boolean isRequired) {
        return this.addOption(type, name, description, isRequired, false);
    }

    /**
     * Add an option with autocomplete support
     * @param type The option type
     * @param name The option name
     * @param description The option description
     * @param isRequired Whether the option is required
     * @param autoComplete The autocomplete provider
     * @return The SlashCommandData for chaining
     */
    protected final SlashCommandData addOption(OptionType type, String name, String description, boolean isRequired, AutoCompleteProvider autoComplete) {
        final SlashCommandData cmd = this.addOption(type, name, description, isRequired, true);
        if(autoComplete != null) AutoCompleteRegistry.register(this.CMD_NAME, name, autoComplete);
        return cmd;
    }
    
    /**
     * Add an option with autocomplete support
     * @param type The option type
     * @param name The option name
     * @param description The option description
     * @param isRequired Whether the option is required
     * @param autoCompleteEnum The autocomplete enum provider
     * @return The SlashCommandData for chaining
     */
    protected final SlashCommandData addOption(OptionType type, String name, String description, boolean isRequired, Class<? extends Enum<?>> autoCompleteEnum) {
        final SlashCommandData cmd = this.addOption(type, name, description, isRequired, true);
        if(autoCompleteEnum != null) AutoCompleteRegistry.registerFromEnum(this.CMD_NAME, name, autoCompleteEnum);
        return cmd;
    }

    /**
     * Add an option with autocomplete support
     * @param type The option type
     * @param name The option name
     * @param description The option description
     * @param isRequired Whether the option is required
     * @param choicesProvider The autocomplete choices provider
     * @return The SlashCommandData for chaining
     */
    protected final SlashCommandData addOption(OptionType type, String name, String description, boolean isRequired, Function<Void, Iterable<String>> choicesProvider) {
        final SlashCommandData cmd = this.addOption(type, name, description, isRequired, true);
        if(choicesProvider != null) AutoCompleteRegistry.registerFromCollection(this.CMD_NAME, name, choicesProvider);
        return cmd;
    }

    private final SlashCommandData addOption(OptionType type, String name, String description, boolean isRequired, boolean autoComplete) {
        return this.data().addOption(type, name, description, isRequired, autoComplete);
    }

    public final void register() {
        CommandsManager.COMMANDS.put(this.CMD_NAME, this);
    }

    /**
     * Check if the command is used in the official guild, and reply with an error message if not
     * @param event The SlashCommandInteractionEvent to check
     * @return true if the command is used in the official guild, false otherwise
     */
    public final boolean isOfficialGuild(SlashCommandInteractionEvent event) {
        final Guild GUILD = event.getGuild();
        if (!isGlobal() && (GUILD == null || !BotEngine.isOfficialGuild(GUILD))) {
            event.reply(TranslationManager.format(event.getUser().getId(), "command.reply.on_official_guild")).setEphemeral(true).queue();
            return false;
        }
        return true; // Otherwise, we assume it's the official guild
    }

    /**
     * Check if the command is used in the console channel, and reply with an error message if not
     * @param event The SlashCommandInteractionEvent to check
     * @return true if the command is used in the console channel, false otherwise
     */
    public final boolean isConsoleChannel(SlashCommandInteractionEvent event) {
        final GuildChannel CHANNEL = event.getGuildChannel();
        if(!isGlobal() && (CHANNEL == null || !BotEngine.isConsoleChannel(CHANNEL))) {
            event.reply(TranslationManager.format(event.getUser().getId(), "command.reply.on_console")).setEphemeral(true).queue();
            return false;
        }
        return true; // Otherwise, we assume it's the console channel
    }

    /**
     * Is this command global (available in private messages) ?
     * @return true if global, false if guild only
     */
    public boolean isGlobal() { return false; }

    public abstract void handle(SlashCommandInteractionEvent event);
}
