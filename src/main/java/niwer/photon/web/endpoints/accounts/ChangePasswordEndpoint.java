package niwer.photon.web.endpoints.accounts;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
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
            handler.status(HttpStatus.BAD_REQUEST).result("Invalid password change payload");
            return;
        }

        if (request == null || request.email == null || request.currentPassword == null || request.newPassword == null) {
            handler.status(HttpStatus.BAD_REQUEST).result("Missing parameters");
            return;
        }

        if (request.email.isBlank() || request.currentPassword.isBlank() || request.newPassword.isBlank()) {
            handler.status(HttpStatus.BAD_REQUEST).result("Parameters cannot be blank");
            return;
        }

        if (request.newPassword.length() < 8) {
            handler.status(HttpStatus.BAD_REQUEST).result("Password must be at least 8 characters long");
            return;
        }

        final ObjectUserAccount account = PlayerAccountTable.getAccountByEmail(request.email);
        if (account == null) {
            handler.status(HttpStatus.NOT_FOUND).result("Account not found");
            return;
        }

        if (!HashUtils.passwordMatches(account.password(), request.currentPassword)) {
            handler.status(HttpStatus.UNAUTHORIZED).result("Incorrect password");
            return;
        }

        if (account.isTotpEnabled()) {
            if (request.code == null || request.code.isBlank()) {
                handler.status(HttpStatus.FORBIDDEN).result("2FA code required");
                return;
            }

            if (!TotpManager.verifyCodeWithRecovery(account.getTotpSecret(), request.code, account.getUuid())) {
                handler.status(HttpStatus.UNAUTHORIZED).result("Invalid 2FA code");
                return;
            }
        }

        PlayerAccountTable.setPassword(account.getUuid(), request.newPassword);
        handler.json(account.payload());
    }

    private record PasswordChangeRequest(String email, String currentPassword, String newPassword, String code) {}
}