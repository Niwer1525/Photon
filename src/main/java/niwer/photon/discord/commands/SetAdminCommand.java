package niwer.photon.discord.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import niwer.photon.Directories;
import niwer.photon.discord.BotEngine;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.sql.PlayerAccountTable.AccountDeletionStatus;

/**
 * @author Niwer
 */
@SuppressWarnings("null") // The compiler in Photon is not good at handling JDA's @Nonnull annotations, so we suppress null warnings in this class
public class SetAdminCommand extends AbstractSlashCommand {
    public SetAdminCommand() {
        super("set_admin", "Sets a user as an admin on the web panel.");
        this.addOption(OptionType.STRING, "account_uuid", "The UUID of the account to set as admin", true);
        this.addOption(OptionType.BOOLEAN, "administrator", "Whether to set the user as an admin", true);
        this.data().setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)); // Only allow administrators to use this command
    }


    @Override
    public void handle(SlashCommandInteractionEvent event) {
        /* Check if the user is in the official guild */
        if(!BotEngine.isOfficialGuild(event.getGuild())) {
            event.reply("This command can only be used in the official Discord server.").setEphemeral(true).queue();
            return;
        }

        /* Check if the user is in the correct channel */
        if(event.getChannel().getIdLong() != Long.parseLong(Directories.getConfig().network_console_channel_id)) {
            event.reply("This command can only be used in the network console channel.").setEphemeral(true).queue();
            return;
        }

        /* Get the UUID of the account to set as admin */
        final String UUID = event.getOption("account_uuid").getAsString();
        if(UUID == null || UUID.isEmpty()) {
            event.reply("You must provide a valid account UUID.").setEphemeral(true).queue();
            return;
        }

        /* Get the admin status */
        final boolean IS_ADMIN = event.getOption("administrator") != null && event.getOption("administrator").getAsBoolean();

        /* Get the Player Account */
        final ObjectUserAccount ACCOUNT = PlayerAccountTable.getAccountByUUID(UUID.trim());
        if(ACCOUNT == null) {
            event.reply("No account found with the provided UUID: `" + UUID + "`.").setEphemeral(true).queue();
            return;
        }
        if(ACCOUNT.isAdministrator() == IS_ADMIN) {
            event.reply("The account with UUID `" + UUID + "` is already " + (IS_ADMIN ? "an admin" : "not an admin") + ".").setEphemeral(true).queue();
            return;
        }
        if(ACCOUNT.getDeletionStatus() == AccountDeletionStatus.PENDING_DELETION) {
            event.reply("The account with UUID `" + UUID + "` is pending deletion and cannot be modified.").setEphemeral(true).queue();
            return;
        }
        if(!ACCOUNT.isTotpEnabled()) {
            event.reply("The account with UUID `" + UUID + "` does not have 2FA enabled. For security reasons, you cannot set an account as admin without 2FA enabled.").setEphemeral(true).queue();
            return;
        }

        /* Set the account as admin */
        PlayerAccountTable.setAdmin(UUID.trim(), IS_ADMIN);
        event.reply("Successfully set the account with UUID `" + UUID + "` as " + (IS_ADMIN ? "an admin" : "not an admin") + ".").setEphemeral(true).queue();
    }
}
