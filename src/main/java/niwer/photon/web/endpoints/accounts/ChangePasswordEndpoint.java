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

public class ChangePasswordEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/change_password"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);

        final PasswordChangeRequest request;
        try {
            request = GsonUtils.GSON.fromJson(handler.body(), PasswordChangeRequest.class);
        } catch (Exception exception) {
            handler.status(400).result("Invalid password change payload");
            return;
        }

        if (request == null || request.email == null || request.currentPassword == null || request.newPassword == null) {
            handler.status(400).result("Missing parameters");
            return;
        }

        if (request.email.isBlank() || request.currentPassword.isBlank() || request.newPassword.isBlank()) {
            handler.status(400).result("Parameters cannot be blank");
            return;
        }

        if (request.newPassword.length() < 8) {
            handler.status(400).result("Password must be at least 8 characters long");
            return;
        }

        final ObjectUserAccount account = PlayerAccountTable.getAccountByEmail(request.email);
        if (account == null) {
            handler.status(404).result("Account not found");
            return;
        }

        if (!HashUtils.passwordMatches(account.password(), request.currentPassword)) {
            handler.status(401).result("Incorrect password");
            return;
        }

        // --- 2FA Verification Check ---
        if (account.isTotpEnabled()) {
            if (request.code == null || request.code.isBlank()) {
                handler.status(403).result("2FA code required");
                return;
            }

            if (!TotpManager.verifyCode(account.getTotpSecret(), request.code)) {
                handler.status(401).result("Invalid 2FA code");
                return;
            }
        }

        PlayerAccountTable.setPassword(account.getUuid(), request.newPassword);
        handler.json(account.payload());
    }

    private record PasswordChangeRequest(String email, String currentPassword, String newPassword, String code) {}
}