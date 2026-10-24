package niwer.photon.web.endpoints.admin;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.PhotonEngine;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AdminTableDataEndpoint implements IEndpoint {

    @Override public String path() { return "/admin/tables/{table}"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);

        try {
            if (SessionManager.requireAdministrator(handler) == null) return;
    
            final String TABLE_NAME = handler.pathParam("table");
            final AdminTablesEndpoint.TableInfo TABLE_INFO = AdminTablesEndpoint.getTables().stream()
                .filter(info -> info.table().equalsIgnoreCase(TABLE_NAME))
                .findFirst()
                .orElse(null);
    
            if (TABLE_INFO == null) {
                handler.status(404).result("Unknown table");
                return;
            }
    
            final int LIMIT = clampLimit(handler.queryParam("limit"));
            final List<String> COLUMNS = new ArrayList<>();
            final List<Map<String, Object>> ROWS = new ArrayList<>();
    
            try {
                PhotonEngine.DATA_BASE.connect();
                try (Statement STATEMENT = PhotonEngine.DATA_BASE.sqlConnection().createStatement(); ResultSet RESULT = STATEMENT.executeQuery("SELECT * FROM \"" + TABLE_INFO.table() + "\" LIMIT " + LIMIT)) {
    
                    final ResultSetMetaData METADATA = RESULT.getMetaData();
                    for (int column = 1; column <= METADATA.getColumnCount(); column++) COLUMNS.add(METADATA.getColumnName(column));
    
                    while (RESULT.next()) {
                        final Map<String, Object> ROW = new LinkedHashMap<>();
                        for (int column = 1; column <= METADATA.getColumnCount(); column++) ROW.put(METADATA.getColumnName(column), normalizeValue(RESULT.getObject(column)));
                        ROWS.add(ROW);
                    }
                }
            } catch (Exception e) {
                handler.status(500).result("Failed to load table: " + e.getMessage());
                return;
            }
    
            handler.json(new TableData(TABLE_INFO.table(), TABLE_INFO.label(), COLUMNS, ROWS));
        } catch (Exception e) {
            IEndpoint.handleException(AdminTableDataEndpoint.class, handler, e);
        }
    }

    private static int clampLimit(String limitValue) {
        final int DEFAULT_LIMIT = 250;
        if (limitValue == null || limitValue.isBlank()) return DEFAULT_LIMIT;

        try {
            return Math.max(1, Math.min(Integer.parseInt(limitValue), 500));
        } catch (NumberFormatException e) {
            return DEFAULT_LIMIT;
        }
    }

    private static Object normalizeValue(Object value) { return (value instanceof Date date) ? date.toString() : value; }

    public record TableData(String table, String label, List<String> columns, List<Map<String, Object>> rows) {}
}