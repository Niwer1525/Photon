package niwer.photon.web.endpoints.accounts;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.util.GsonUtils;
import niwer.photon.util.HashUtils;
import niwer.photon.util.TotpManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

/**
 * Endpoint to update a user's profile information, including username, email, and password.
 * 
 * @author Niwer
 */
public class UpdateAccountSettingsEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/update_profile"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);

        final ProfileUpdateRequest request;
        try {
            request = GsonUtils.GSON.fromJson(handler.body(), ProfileUpdateRequest.class);
        } catch (Exception exception) {
            handler.status(400).result("Invalid profile payload");
            return;
        }

        if (request == null || request.uuid == null || request.currentPassword == null) {
            handler.status(400).result("Missing parameters");
            return;
        }

        if (request.uuid.isBlank() || request.currentPassword.isBlank()) {
            handler.status(400).result("Parameters cannot be blank");
            return;
        }

        final ObjectUserAccount account = PlayerAccountTable.getAccountByUUID(request.uuid);
        if (account == null) {
            handler.status(404).result("Account not found");
            return;
        }

        /* Enforce 2FA check if enabled */
        if (account.isTotpEnabled()) {
            if (request.code == null || request.code.isBlank()) {
                handler.status(400).result("Two-factor authentication code is required");
                return;
            }

            if (!TotpManager.verifyCodeWithRecovery(account.getTotpSecret(), request.code.trim(), account.getUuid())) {
                handler.status(401).result("Invalid 2FA code");
                return;
            }
        }

        if (!HashUtils.passwordMatches(account.password(), request.currentPassword)) {
            handler.status(401).result("Incorrect password");
            return;
        }

        final String nextUsername = request.username != null ? request.username.trim() : account.getUsername();
        final String nextEmail = request.email != null ? request.email.trim().toLowerCase() : account.getEmail();
        final String nextPassword = request.newPassword != null ? request.newPassword : null;

        if (nextUsername.isBlank() || nextEmail.isBlank()) {
            handler.status(400).result("Username and email cannot be blank");
            return;
        }

        if (!nextUsername.equalsIgnoreCase(account.getUsername())) {
            final ObjectUserAccount existingUsername = PlayerAccountTable.getAccountByUsername(nextUsername);
            if (existingUsername != null && !existingUsername.getUuid().equals(account.getUuid())) {
                handler.status(400).result("An account with this username already exists.");
                return;
            }
            PlayerAccountTable.setUsername(account.getUuid(), nextUsername);
        }

        if (!nextEmail.equalsIgnoreCase(account.getEmail())) {
            final ObjectUserAccount existingEmail = PlayerAccountTable.getAccountByEmail(nextEmail);
            if (existingEmail != null && !existingEmail.getUuid().equals(account.getUuid())) {
                handler.status(400).result("An account with this email already exists.");
                return;
            }
            PlayerAccountTable.setEmail(account.getUuid(), nextEmail);
        }

        if (nextPassword != null && !nextPassword.isBlank()) {
            if (nextPassword.length() < 8) {
                handler.status(400).result("Password must be at least 8 characters long");
                return;
            }
            if (request.confirmPassword == null || !nextPassword.equals(request.confirmPassword)) {
                handler.status(400).result("Passwords do not match");
                return;
            }
            PlayerAccountTable.setPassword(account.getUuid(), nextPassword);
        }

        final ObjectUserAccount updatedAccount = PlayerAccountTable.getAccountByUUID(account.getUuid());
        handler.json(updatedAccount == null ? account.payload() : updatedAccount.payload());
    }

    private record ProfileUpdateRequest(
        String uuid,
        String currentPassword,
        String username,
        String email,
        String newPassword,
        String confirmPassword,
        String code
    ) {}
}
