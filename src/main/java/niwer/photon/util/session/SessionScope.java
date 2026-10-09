package niwer.photon.util.session;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.reflect.TypeToken;

import io.javalin.http.Context;
import niwer.photon.Directories;
import niwer.photon.util.GsonUtils;

public enum SessionScope {
    ADMIN("admin_sessions.json", "photon_admin", true),
    USER("user_sessions.json", "X-Photon-User-Token", false);

    private static final Type SESSION_MAP_TYPE = new TypeToken<Map<String, SessionSnapshot>>() {}.getType();
    
    protected final File file;
    protected final String headerOrCookie;
    protected final boolean isCookie;
    protected final Map<String, SessionSnapshot> sessions = new ConcurrentHashMap<>();

    SessionScope(String filename, String headerOrCookie, boolean isCookie) {
        this.file = new File(Directories.BASE_DIR, filename);
        this.headerOrCookie = headerOrCookie;
        this.isCookie = isCookie;
    }

    protected synchronized void load() {
        sessions.clear();
        if (!file.exists()) return;
        try (FileReader reader = new FileReader(file)) {
            Map<String, SessionSnapshot> loaded = GsonUtils.GSON.fromJson(reader, SESSION_MAP_TYPE);
            if (loaded != null) sessions.putAll(loaded);
        } catch (Exception ignored) {}
    }

    protected synchronized void save() {
        if (!Directories.BASE_DIR.exists()) Directories.BASE_DIR.mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            GsonUtils.GSON.toJson(sessions, SESSION_MAP_TYPE, writer);
        } catch (Exception ignored) {}
    }

    /**
     * Extracts the session token from the request context, either from a cookie or a header, depending on the scope's configuration.
     * If the token is not found in the expected location, it will also check for a Bearer token in the Authorization header.
     * 
     * @param ctx The Javalin context containing the request and response information
     * @return The extracted session token as a String, or null if no token is found
     */
    public String extractToken(Context ctx) {
        String token = isCookie ? ctx.cookie(headerOrCookie) : ctx.header(headerOrCookie);
        if (token != null && !token.isBlank()) return token.trim();

        String auth = ctx.header("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7).trim();
        return null;
    }
}
