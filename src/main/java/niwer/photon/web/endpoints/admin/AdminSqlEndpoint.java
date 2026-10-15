package niwer.photon.web.endpoints.admin;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.javalin.http.Context;
import niwer.photon.PhotonEngine;
import niwer.photon.util.GsonUtils;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.EndpointUtils;
import niwer.photon.web.endpoints.IEndpoint;

/**
 * TODO: This endpoint is a prototype made to be as safe as possible.
 * AdminSqlEndpoint
 */
public class AdminSqlEndpoint implements IEndpoint {
    private static final int MAX_SQL_LENGTH = 2000;
    private static final int MAX_ROWS = 100;
    private static final Pattern SAFE_SQL = Pattern.compile("[\\x20-\\x7E\\r\\n\\t]+");
    private static final Pattern TABLE = Pattern.compile("(?i)\\b(?:FROM|UPDATE|JOIN)\\s+\"?([A-Za-z][A-Za-z0-9_]*)\"?");

    @Override public String path() { return "/api/admin/sql"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);
        
        if (SessionManager.requireAdministrator(handler) == null) return;
        if (!SessionManager.validateCsrf(handler)) {
            handler.status(403).result("Invalid CSRF token");
            return;
        }

        /* Get the SQL code entered by the user */
        String sql = GsonUtils.getString(EndpointUtils.readBody(handler), "sql", "sql", null);
        if (sql == null) {
            handler.status(400).result("SQL is required");
            return;
        }
        sql = sql.trim();
        
        /* Ensure it's a valid SQL code, if not cancel. */
        String error = validate(sql);
        if (error != null) {
            handler.status(400).result(error);
            return;
        }

        boolean write = sql.regionMatches(true, 0, "UPDATE", 0, 6); // If it's an UPDATE statement, we will create a backup before executing it. SELECT statements are read-only and don't require a backup.
        try {
            synchronized (PhotonEngine.DATA_BASE) {
                PhotonEngine.DATA_BASE.connect();
                if (write) PhotonEngine.backupManager().createBackup("admin-before-sql");
                try (Statement STATEMENT = PhotonEngine.DATA_BASE.sqlConnection().createStatement()) {
                    if (write) {
                        int changed = STATEMENT.executeUpdate(sql);
                        handler.json(Map.of("type", "update", "affectedRows", changed));
                    } else {
                        try (ResultSet result = STATEMENT.executeQuery(sql)) {
                            handler.json(readRows(result));
                        }
                    }
                }
            }
        } catch (Exception exception) {
            handler.status(400).result("SQL rejected: " + exception.getMessage());
        }
    }

    private static String validate(String sql) {
        if (sql.isBlank() || sql.length() > MAX_SQL_LENGTH || !SAFE_SQL.matcher(sql).matches())
            return "SQL is empty, too long, or contains unsafe characters";
        if (sql.endsWith(";") || sql.contains(";") || sql.contains("--") || sql.contains("/*") || sql.contains("*/"))
            return "Only one statement is allowed";

        String upper = sql.toUpperCase(Locale.ROOT);
        boolean select = upper.startsWith("SELECT ");
        boolean update = upper.startsWith("UPDATE ");
        if (!select && !update) return "Only SELECT and UPDATE are allowed";
        if (upper.matches("(?s).*\\b(PRAGMA|ATTACH|DETACH|VACUUM|REINDEX|WITH|INSERT|DELETE|DROP|ALTER|CREATE|REPLACE|TRIGGER)\\b.*"))
            return "This SQL operation is not allowed";

        var tableMatcher = TABLE.matcher(sql);
        while (tableMatcher.find()) {
            if (AdminTablesEndpoint.getTables().stream().noneMatch(info -> info.table().equalsIgnoreCase(tableMatcher.group(1))))
                return "The table is not available for this operation: " + tableMatcher.group(1);
        }
        if (select) {
            if (!upper.matches("(?s).*\\bLIMIT\\s+(\\d+)\\s*$")) return "SELECT requires LIMIT (maximum 100)";
            int limit = Integer.parseInt(upper.replaceFirst("(?s).*\\bLIMIT\\s+(\\d+)\\s*$", "$1"));
            if (limit < 1 || limit > MAX_ROWS) return "SELECT LIMIT must be between 1 and 100";
        } else if (!upper.contains(" WHERE ") || !upper.matches("(?s).*\\bLIMIT\\s+1\\s*$")) {
            return "UPDATE requires WHERE and LIMIT 1";
        }
        return null;
    }

    private static Map<String, Object> readRows(ResultSet result) throws Exception {
        ResultSetMetaData metadata = result.getMetaData();
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) columns.add(metadata.getColumnName(i));
        List<Map<String, Object>> rows = new ArrayList<>();
        while (result.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= metadata.getColumnCount(); i++) row.put(columns.get(i - 1), result.getObject(i));
            rows.add(row);
        }
        return Map.of("type", "select", "columns", columns, "rows", rows);
    }
}
