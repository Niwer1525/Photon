package niwer.photon.discord.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.util.TranslationManager;
import niwer.photon.util.TranslationManager.Language;
import niwer.queryon.QueryonException;

/**
 * Command to change the language of the bot responses for a user.
 * Avaliable globally.
 * 
 * @author Niwer
 */
@SuppressWarnings("null") // The compiler in Photon is not good at handling JDA's @Nonnull annotations, so we suppress null warnings in this class
public class LanguageCommand extends AbstractSlashCommand {

    public LanguageCommand() {
        super("lang", "Change the language of the bot responses.");
        this.addOption(OptionType.STRING, "language", "Language to set", true, Language.class);
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        final OptionMapping LANGUAGE_ARG = event.getOption("language");
        final var LANGUAGE = Language.fromString(LANGUAGE_ARG.getAsString());

        /* Set the new user language */
        try {
            PlayerAccountTable.setLanguageFromUUID(event.getUser().getId(), LANGUAGE);
            event.reply(TranslationManager.format(event.getUser().getId(), "command.reply.language.success")).queue();
        } catch (QueryonException e) {
            event.reply(TranslationManager.format(event.getUser().getId(), "command.reply.language.failure")).queue();
        }
    }
}