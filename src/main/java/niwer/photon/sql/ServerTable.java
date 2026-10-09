package niwer.photon.sql;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectServer;
import niwer.photon.util.PhotonLogTypes;
import niwer.queryon.DataBase;
import niwer.queryon.queries.Expressions;
import niwer.queryon.queries.interaction.InsertionManager;
import niwer.queryon.queries.interaction.SelectionManager;
import niwer.queryon.queries.interaction.UpdateManager;
import niwer.queryon.tables.Table;

public class ServerTable extends Table {

    public static final long SERVER_VISIBILITY_TTL_MILLIS = 30L * 24L * 60L * 60L * 1000L; // 30 days

    public ServerTable(DataBase db) {
        super(db);
        this.addColumnsFromClass(ObjectServer.class).execute();
    }

    @Override public String name() { return "Server"; }

    public static void saveOrUpdate(ObjectServer server) {
        if (server == null || server.serverIP == null || server.serverIP.isBlank()) return;
        if (server.serverPort <= 0) return;

        final Date now = new Date();
        if (exists(server.serverIP, server.serverPort)) {
            UpdateManager.update(PhotonEngine.DATA_BASE, ServerTable.class)
                .set("server_name", server.serverName)
                .set("server_motd", server.serverMOTD)
                .set("queue_port", server.queuePort)
                .set("last_seen_at", now)
                .set("site_url", server.site)
                .set("discord", server.discord)
                .set("project_id", server.projectId)
                .where(Expressions.isEqualTo("server_ip", server.serverIP))
                .where(Expressions.isEqualTo("server_port", server.serverPort))
                .execute();
        } else {
            InsertionManager.insert(PhotonEngine.DATA_BASE, ServerTable.class, server)
                .row(server.serverName, server.serverMOTD, server.serverIP, server.serverPort, server.queuePort, now, server.site, server.discord)
                .execute();
        }
    }

    /**
     * Get the list of visible servers. This method also performs cleanup of expired servers based on the defined TTL.
     * 
     * @return A list of ObjectServer instances that are currently visible (i.e., have been seen within the TTL period)
     */
    public static List<ObjectServer> getVisibleServers() {
        try {
            final Date CUT_OFF = new Date(System.currentTimeMillis() - SERVER_VISIBILITY_TTL_MILLIS);
            return getAllServers().stream().filter(server -> server != null && server.last_seen_at != null && !server.last_seen_at.before(CUT_OFF)).toList();
        } catch (Exception e) {
            Console.log("Failed to load server list: " + e.getMessage()).type(PhotonLogTypes.SQL).error().container(PhotonEngine.LOGGER).send();
            return new ArrayList<>();
        }
    }
    
    public static List<ObjectServer> getAllServers() {
        try {
            return SelectionManager.select(PhotonEngine.DATA_BASE, ServerTable.class).executeList(ObjectServer.class);
        } catch (Exception e) {
            Console.log("Failed to load all servers: " + e.getMessage()).type(PhotonLogTypes.SQL).error().container(PhotonEngine.LOGGER).send();
            return new ArrayList<>();
        }
    }

    /**
     * Get a server by its IP and port. Returns null if not found or if input is invalid.
     * 
     * @param ip The server IP
     * @param port The server port
     * @return The ObjectServer if found, or null if not found or if input is invalid
     */
    public static ObjectServer getServer(String ip, int port) {
        if (ip == null || ip.isBlank() || port <= 0) return null;
        try {
            return SelectionManager.select(PhotonEngine.DATA_BASE, ServerTable.class)
                .where(Expressions.and(
                    Expressions.isEqualTo("server_ip", ip),
                    Expressions.isEqualTo("server_port", port)
                ))
                .limit(1)
                .executeSerializable(ObjectServer.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean exists(String serverIP, int serverPort) {
        try {
            return SelectionManager.select(PhotonEngine.DATA_BASE, ServerTable.class, "COUNT(*) as count")
                .where(Expressions.and(
                    Expressions.isEqualTo("server_ip", serverIP),
                    Expressions.isEqualTo("server_port", serverPort)
                ))
                .executeHasResult();
        } catch (Exception e) {
            return false;
        }
    }
}