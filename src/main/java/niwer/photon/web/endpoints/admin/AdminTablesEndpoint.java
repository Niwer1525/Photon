package niwer.photon.web.endpoints.admin;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AdminTablesEndpoint implements IEndpoint {

    private static final List<TableInfo> TABLES = List.of(
        new TableInfo("Server", "Server", "Saved server status rows"),
        new TableInfo("Account", "Account", "Registered player accounts"),
        new TableInfo("License", "License", "Issued licenses"),
        new TableInfo("HWID", "HWID", "HWID bindings"),
        new TableInfo("CrashReport", "CrashReport", "Stored crash reports"),
        new TableInfo("AnticheatReport", "AnticheatReport", "Stored anti-cheat reports"),
        new TableInfo("DiscordLog", "DiscordLog", "Moderation logs")
    );

    @Override public String path() { return "/api/admin/tables"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);

        try {
            if (SessionManager.requireAdministrator(handler) == null) return;
            handler.json(TABLES);
        } catch (Exception e) {
            IEndpoint.handleException(AdminTablesEndpoint.class, handler, e);
        }
    }

    public static List<TableInfo> getTables() {
        return TABLES;
    }

    public record TableInfo(String table, String label, String description) {}
}