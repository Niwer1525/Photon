package niwer.photon.web.endpoints.servers;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.photon.sql.ServerTable;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class ServerListEndpoint implements IEndpoint {

    @Override public String path() { return "/servers/server-list"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.SECONDS);

        try {
            handler.status(HttpStatus.OK).json(ServerTable.getVisibleServers());
        } catch (Exception e) {
            IEndpoint.handleException(ServerListEndpoint.class, handler, e);
        }
    }
}