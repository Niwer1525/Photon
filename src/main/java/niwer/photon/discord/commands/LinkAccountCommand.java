
package niwer.photon.discord.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.util.TranslationManager;

/**
 * @author Niwer
 */
@SuppressWarnings("null") // The compiler in Photon is not good at handling JDA's @Nonnull annotations, so we suppress null warnings in this class
public class LinkAccountCommand extends AbstractSlashCommand {

    public LinkAccountCommand() {
        super("link-account", "Link your Game account to your Discord account.");
        this.addOption(OptionType.STRING, "uuid", "Your unique user identity", true);
        this.addOption(OptionType.STRING, "code", "Your discord auth code.", true);
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        final String UUID = event.getOption("uuid").getAsString(); // Won't be null because it's required
        final String AUTHCODE = event.getOption("code").getAsString(); // Won't be null because it's required

        /* Check the UUID and the code */
        if (!PlayerAccountTable.isAuthCodeValid(UUID, AUTHCODE)) {
            if (!PlayerAccountTable.existByUUID(UUID)) event.reply(TranslationManager.format(event.getUser().getId(), "command.link_account.failure.no_uuid")).setEphemeral(true).queue();
            else event.reply(TranslationManager.format(event.getUser().getId(), "command.link_account.failure.invalid_code")).setEphemeral(true).queue();
            return;
        }

        /* Check if there's an account with this UUID */
        final ObjectUserAccount PROFILE = PlayerAccountTable.getAccountByUUID(UUID);
        if (PROFILE == null) {
            event.reply(TranslationManager.format(event.getUser().getId(), "command.link_account.failure.no_uuid")).setEphemeral(true).queue();
            return;
        }

        /* Check if the account has already been linked */
        if (PROFILE.hasDiscordLinked()) {
            event.reply(TranslationManager.format(event.getUser().getId(), "command.link_account.failure.already_linked")).setEphemeral(true).queue();
            return;
        }

        /* Check if the discord account has already been linked to another official account */
        final String DISCORD_USER_ID = event.getUser().getId();
        if (PlayerAccountTable.getAccountByDiscordID(DISCORD_USER_ID) != null) {
            event.reply(TranslationManager.format(event.getUser().getId(), "command.link_account.failure.linked_to_other")).setEphemeral(true).queue();
            return;
        }

        /* Update the Discord ID */
        PlayerAccountTable.setDiscordID(UUID, DISCORD_USER_ID);

        /* Print reply */
        event.reply(TranslationManager.format(event.getUser().getId(), "command.link_account.success", event.getUser().getAsMention())).queue();
    }

    @Override public boolean isGlobal() { return true; }
}