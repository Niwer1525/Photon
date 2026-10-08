package niwer.photon.util.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.javalin.http.Context;
import niwer.photon.objects.ObjectUserAccount;
import niwer.photon.sql.PlayerAccountTable;
import niwer.photon.util.HashUtils;
import niwer.photon.util.TotpManager;

public final class SessionManager {

    private static final Map<String, Pending2FA> PENDING_2FA = new ConcurrentHashMap<>();

    static {
        for (SessionScope scope : SessionScope.values()) scope.load();
    }

    private SessionManager() {}

    /**
     * Creates a pending 2FA challenge for the given user UUID and scope, storing it in the PENDING_2FA map with a unique ticket.
     * The challenge will expire after 5 minutes.
     * 
     * @param uuid The UUID of the user for whom the 2FA challenge is being created
     * @param scope The scope of the session (ADMIN or USER) to determine which session map to use
     * @param checkoutToken An optional checkout token associated with the 2FA challenge, if applicable
     * @return
     */
    public static String createPending2FA(String uuid, SessionScope scope, String checkoutToken) {
        String ticket = UUID.randomUUID().toString();
        PENDING_2FA.put(ticket, new Pending2FA(uuid, scope, System.currentTimeMillis(), checkoutToken));
        return ticket;
    }

    /**
     * Completes a pending 2FA challenge by verifying the provided code against the stored secret for the user associated with the given ticket.
     * 
     * @param ticket The unique ticket identifying the pending 2FA challenge
     * @param code The TOTP code provided by the user for verification
     * @return A Session object if the 2FA challenge is successfully completed and the code is valid; null otherwise
     */
    public static Session complete2FA(String ticket, String code) {
        Pending2FA pending = PENDING_2FA.get(ticket);
        if (pending == null) return null;

        // Expire pending challenge after 5 minutes
        if (System.currentTimeMillis() - pending.createdAt() > 300_000) {
            PENDING_2FA.remove(ticket);
            return null;
        }

        ObjectUserAccount account = PlayerAccountTable.getAccountByUUID(pending.uuid());
        if (account == null || !TotpManager.verifyCode(account.getTotpSecret(), code)) return null;

        PENDING_2FA.remove(ticket);
        return createSession(account, pending.scope());
    }

    /**
     * Attempts to log in a user with the provided email and password, creating a new session if successful. Returns an AuthSession object containing the session token and account information, or null if login fails.
     * 
     * @param email The email address of the user attempting to log in
     * @param password The password of the user attempting to log in
     * @param scope The scope of the session (ADMIN or USER) to determine which session map to use
     * @return An AuthSession object containing the session token and account information if login is successful; null otherwise
     */
    public static Session login(String email, String password, SessionScope scope) {
        if (email == null || password == null) return null;

        ObjectUserAccount account = PlayerAccountTable.getAccountByEmail(email);
        if (account == null || account.password() == null) return null;
        if (scope == SessionScope.ADMIN && !account.isAdministrator()) return null;
        if (!HashUtils.passwordMatches(account.password(), password)) return null;

        return createSession(account, scope);
    }

    public static Session createSession(ObjectUserAccount account, SessionScope scope) {
        if (account == null) return null;

        String token = UUID.randomUUID().toString();
        String csrf = (scope == SessionScope.ADMIN) ? UUID.randomUUID().toString() : null;

        scope.sessions.put(token, new SessionSnapshot(account, System.currentTimeMillis(), csrf));
        scope.save();
        return new Session(token, account);
    }

    private static ObjectUserAccount accountFromRequest(Context handler, SessionScope scope) {
        String token = scope.extractToken(handler);
        SessionSnapshot session = (token != null) ? scope.sessions.get(token) : null;

        if (session == null) {
            if(scope == SessionScope.USER) { // If user session is not found, check if an admin session exists and the account has admin privileges
                ObjectUserAccount adminAccount = accountFromRequest(handler, SessionScope.ADMIN);
                return (adminAccount != null && adminAccount.isAdministrator()) ? adminAccount : null;
            }
            if (scope == SessionScope.ADMIN) { // Admins can fall back to regular user session if the account has admin privileges
                ObjectUserAccount userAccount = accountFromRequest(handler, SessionScope.USER);
                return (userAccount != null && userAccount.isAdministrator()) ? userAccount : null;
            }
            return null;
        }

        ObjectUserAccount snapshot = session.account();
        if (snapshot == null || snapshot.getUuid() == null) return null;

        ObjectUserAccount freshAccount = PlayerAccountTable.getAccountByUUID(snapshot.getUuid());
        return (freshAccount != null) ? freshAccount : snapshot;
    }

    /**
     * Requires that the request is made by a logged-in user. If the request is not made by a logged-in user, it will respond with a 401 Unauthorized status code and return null.
     * 
     * @param handler The Javalin context containing the request and response information
     * @return The ObjectUserAccount of the user making the request, or null if the request is not made by a logged-in user
     */
    public static ObjectUserAccount requireAccount(Context handler) {
        ObjectUserAccount account = accountFromRequest(handler, SessionScope.USER);
        if (account == null) handler.status(401).result("Unauthorized");
        return account;
    }

    /**
     * Requires that the request is made by an administrator. If the request is not made by an administrator, it will respond with a 401 Unauthorized or 403 Forbidden status code and return null.
     * 
     * @param handler The Javalin context containing the request and response information
     * @return The ObjectUserAccount of the administrator making the request, or null if the request is not made by an administrator
     */
    public static ObjectUserAccount requireAdministrator(Context handler) {
        ObjectUserAccount account = accountFromRequest(handler, SessionScope.ADMIN);
        if (account == null) {
            handler.status(401).result("Unauthorized");
            return null;
        }
        if (!account.isAdministrator()) {
            handler.status(403).result("Administrator access required");
            return null;
        }
        return account;
    }

    /**
     * Logs out the session associated with the given token and scope.
     * 
     * @param token The session token to invalidate
     * @param scope The scope of the session (ADMIN or USER) to determine which session map to modify
     */
    public static void logout(String token, SessionScope scope) {
        if (token != null && scope.sessions.remove(token) != null) scope.save();
    }

    /**
     * Retrieves the CSRF token associated with a given session token.
     * 
     * @param token The session token for which to retrieve the CSRF token
     * @return The CSRF token associated with the session, or null if the session does not exist or the token is invalid
     */
    public static String getCsrfForToken(String token) {
        if (token == null || token.isBlank()) return null;
        SessionSnapshot session = SessionScope.ADMIN.sessions.get(token);
        return (session != null) ? session.csrf() : null;
    }

    /**
     * Validates the CSRF token provided in the request against the CSRF token stored in the session associated with the request's token.
     * 
     * @param handler The Javalin context containing the request and response information
     * @return True if the CSRF token is valid and matches the session's CSRF token; false otherwise
     */
    public static boolean validateCsrf(Context handler) {
        String token = SessionScope.ADMIN.extractToken(handler);
        if (token == null) return false;

        SessionSnapshot session = SessionScope.ADMIN.sessions.get(token);
        if (session == null || session.csrf() == null) return false;

        String header = handler.header("X-CSRF-Token");
        return session.csrf().equals(header);
    }
}