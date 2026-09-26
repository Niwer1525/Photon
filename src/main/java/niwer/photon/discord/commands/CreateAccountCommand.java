package niwer.photon.discord.commands;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;

/**
 * This command allows users to create a Photon account and link it to their Discord account.
 * It is available globally.
 * 
 * @author Niwer
 */
public class CreateAccountCommand extends AbstractModalCommand {

    public CreateAccountCommand() {
        // super("create-account", "Create a new Photon account and link it to your Discord account. Alternatively, you can use the /link command if you already have a Photon account.");
        super("create-account",
            "Create a new Photon account and link it to your Discord account.",
            "Account Registration",

            new TextInputField("username", "Username", true)
                .setPlaceholder("JohnDoe123"),

            new TextInputField("email", "Email", true)
                .setPlaceholder("john.doe@example.com"),

            new TextInputField("password", "Password", true)
                .setMinLength(8)
                .setPlaceholder("Enter your password (min 8 characters)"),
                
            new TextInputField("password_confirm", "Confirm Password", true)
                .setMinLength(8)
                .setPlaceholder("Confirm your password")
        );
    }

    @Override
    public void handleForm(ModalInteractionEvent event) {
        final ModalMapping USERNAME_ARG = event.getValue("username");
        final ModalMapping EMAIL_ARG = event.getValue("email");
        final ModalMapping PASSWORD_ARG = event.getValue("password");
        final ModalMapping PASSWORD_CONFIRM_ARG = event.getValue("password_confirm");
        if(USERNAME_ARG == null || EMAIL_ARG == null || PASSWORD_ARG == null || PASSWORD_CONFIRM_ARG == null) {
            event.reply("Missing required parameters.").setEphemeral(true).queue();
            return;
        }

        /* Ensure the password and confirmation match */
        final String PASSWORD = PASSWORD_ARG.getAsString();
        final String PASSWORD_CONFIRM = PASSWORD_CONFIRM_ARG.getAsString();
        if(!PASSWORD.equals(PASSWORD_CONFIRM)) {
            event.reply("Password and confirmation do not match.").setEphemeral(true).queue();
            return;
        }
        
        /* Ensure no account is already linked to this Discord user */
        final long DISCORD_ID = event.getUser().getIdLong();
        if(PlayerAccountTable.discordExists(DISCORD_ID)) {
            event.reply("You already have an account linked to your Discord account.").setEphemeral(true).queue();
            return;
        }

        /* Ensure the email address is valid and ensure it's not already in use */
        final String EMAIL = EMAIL_ARG.getAsString();
        if(!validEmailAddress(EMAIL)) {
            event.reply("Invalid email address.").setEphemeral(true).queue();
            return;
        }
        if(PlayerAccountTable.emailExists(EMAIL)) {
            event.reply("An account with this email already exists. You can use the /link command to link it to your Discord account.").setEphemeral(true).queue();
            return;
        }

        /* Ensure the username is valid */  
        final String USERNAME = USERNAME_ARG.getAsString();
        if(PlayerAccountTable.usernameExists(USERNAME)) {
            event.reply("This username is already taken. 😔").setEphemeral(true).queue();
            return;
        }

        /* Create the account */
        final ObjectUserAccount ACCOUNT = PlayerAccountTable.createAccount(USERNAME, EMAIL, PASSWORD);
        if(ACCOUNT == null) {
            event.reply("An error occurred while creating your account. Please try again later.").setEphemeral(true).queue();
            return;
        }

        /* Link the account to the Discord user */
        PlayerAccountTable.setDiscordID(ACCOUNT.getUuid(), String.valueOf(DISCORD_ID));

        /* Reply to the user */
        event.reply("Your account has been created and linked to your Discord account!").setEphemeral(true).queue();
    }

    private static boolean validEmailAddress(String email) {
        // Simple regex for email validation
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email != null && email.matches(emailRegex);
    }
}