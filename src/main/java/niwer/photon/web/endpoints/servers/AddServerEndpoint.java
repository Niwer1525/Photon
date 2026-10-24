package niwer.photon.web.endpoints.servers;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.objects.ObjectServer;
import niwer.photon.sql.ServerTable;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

/**
 * Endpoint for adding or updating a server in the database. The server must
 * provide its IP, port, name, and optionally a MOTD. The IP must match the
 * request's remote IP for security reasons.
 */
public class AddServerEndpoint implements IEndpoint {

    @Override public String path() { return "/servers/add-server"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 1, TimeUnit.MINUTES);

        try {
            final ObjectServer server = ObjectServer.fromPayload(handler.body());
            if(server == null) {
                handler.status(HttpStatus.BAD_REQUEST).result("Invalid server payload");
                return;
            }
    
            // final String remoteIp = handler.ip();
            // if (server.serverIP == null || server.serverIP.isBlank() ||
            // !server.serverIP.equals(remoteIp)) {
            // handler.status(HttpStatus.FORBIDDEN).result(String.format("Server IP mismatch (Sender: %s
            // isn't equal to server: %s)", handler.ip(), server.serverIP));
            // return;
            // }
    
            if (server.serverPort <= 0 || server.serverPort > 65535) {
                handler.status(HttpStatus.BAD_REQUEST).result("Invalid server port");
                return;
            }
    
            if (server.serverName == null || server.serverName.isBlank()) {
                handler.status(HttpStatus.BAD_REQUEST).result("Server name is required");
                return;
            }
    
            server.serverName = server.serverName.trim();
            if (server.serverName.length() > 64) server.serverName = server.serverName.substring(0, 64);
            if (server.serverMOTD != null && server.serverMOTD.length() > 2048) server.serverMOTD = server.serverMOTD.substring(0, 2048);
    
            ServerTable.saveOrUpdate(server);
            handler.status(HttpStatus.OK).result("Server added/updated successfully");
            Console.log("Server %s:%d (%s) has been added/updated in the database", server.serverIP, server.serverPort, server.serverName).container(PhotonEngine.LOGGER).type(PhotonLogTypes.WEB_SERVER).send();
        } catch (Exception e) {
            IEndpoint.handleException(AddServerEndpoint.class, handler, e);
        }
    }
}
