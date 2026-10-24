package niwer.photon.web.endpoints.accounts;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.sql.PurchaseTable;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.util.session.Session;
import niwer.photon.util.session.SessionManager;
import niwer.photon.util.session.SessionScope;
import niwer.photon.util.stripe.EntitlementManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.EndpointUtils;
import niwer.photon.web.endpoints.IEndpoint;

/**
 * Endpoint to handle account creation. This is a placeholder implementation and should be properly implemented with necessary validations, password hashing, and database storage.
 * 
 * @author Niwer
 */
public class CreateAccountEndpoint implements IEndpoint {

    @Override public String path() { return "/accounts/create_account"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);
        
        try {
            final String USERNAME = handler.formParam("username");
            final String EMAIL = handler.formParam("email");
            final String PASSWORD = handler.formParam("password");
            final String CHECKOUT_SESSION_ID = EndpointUtils.firstNonBlank(handler.formParam("checkoutSessionId"), handler.formParam("token"));
    
            /* Ensure all parameters are provided */
            if(USERNAME == null || EMAIL == null || PASSWORD == null) {
                handler.status(HttpStatus.BAD_REQUEST).result("Missing parameters");
                return;
            }
    
            /* Ensure no parameters are blank */
            if(USERNAME.isBlank() || EMAIL.isBlank() || PASSWORD.isBlank()) {
                handler.status(HttpStatus.BAD_REQUEST).result("Parameters cannot be blank");
                return;
            }
    
            /* Ensure the email address is valid */
            if(!validEmailAddress(EMAIL)) {
                handler.status(HttpStatus.BAD_REQUEST).result("Invalid email address");
                return;
            }
    
            /* Ensure the password is at least 8 characters long */
            if(PASSWORD.length() < 8) {
                handler.status(HttpStatus.BAD_REQUEST).result("Password must be at least 8 characters long");
                return;
            }
    
            /* Ensure the email address is not already in use */
            if(PlayerAccountTable.emailExists(EMAIL)) {
                handler.status(HttpStatus.BAD_REQUEST).result("An account with this email already exists. Sign in instead.");
                return;
            }
    
            /* Ensure the username is not already in use */
            if(PlayerAccountTable.usernameExists(USERNAME)) {
                handler.status(HttpStatus.BAD_REQUEST).result("An account with this username already exists.");
                return;
            }
    
            /* Check the validity of the purchase token if a token is provided */
            final boolean HAS_PURCHASE_REFERENCE = CHECKOUT_SESSION_ID != null && !CHECKOUT_SESSION_ID.isBlank();
            if (HAS_PURCHASE_REFERENCE && !PurchaseTable.canRedeem(CHECKOUT_SESSION_ID)) {
                handler.status(HttpStatus.FORBIDDEN).result("Invalid or expired purchase token");
                return;
            }
    
            final ObjectUserAccount ACCOUNT = PlayerAccountTable.createAccount(USERNAME, EMAIL, PASSWORD);
            if(ACCOUNT == null) {
                handler.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to create account");
                return;
            }
    
            if (HAS_PURCHASE_REFERENCE && !EntitlementManager.redeemPurchase(CHECKOUT_SESSION_ID, ACCOUNT)) {
                handler.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to link purchase token");
                return;
            }
    
            final Session SESSION = SessionManager.login(EMAIL, PASSWORD, SessionScope.USER);
            if (SESSION == null) {
                handler.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Failed to create session");
                return;
            }
    
            handler.status(HttpStatus.OK).json(new LoginResponse(SESSION.token(), SESSION.refreshToken(), ACCOUNT.payload()));
            Console.log("A new account was created: %s (UUID: %s)", ACCOUNT.getUsername(), ACCOUNT.getUuid()).sendToProcessor().type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
        } catch (Exception e) {
            IEndpoint.handleException(CreateAccountEndpoint.class, handler, e);
        }
    }

    private static boolean validEmailAddress(String email) {
		final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
        return EMAIL_PATTERN.matcher(email).matches();
	}

    private record LoginResponse(String token, String refreshToken, Object account) {}
}
