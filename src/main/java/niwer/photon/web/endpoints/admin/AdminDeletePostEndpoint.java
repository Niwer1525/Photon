package niwer.photon.web.endpoints.admin;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.sql.BlogPostsTable;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.util.session.SessionManager;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AdminDeletePostEndpoint implements IEndpoint {

    @Override public String path() { return "/admin/blog/posts/delete/{uuid}"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 15, TimeUnit.MINUTES);

        try {
            if (SessionManager.requireAdministrator(handler) == null) return;
            
            int uuid;
            try {
                uuid = Integer.parseInt(handler.pathParam("uuid"));
            } catch (NumberFormatException e) {
                handler.status(HttpStatus.BAD_REQUEST).result("Missing uuid parameter");
                return;
            }
    
            BlogPostsTable.deletePostById(uuid);
            handler.status(HttpStatus.OK).result("Blog post deleted successfully");
            Console.log("Blog post (ID: %d) deleted!", uuid).container(PhotonEngine.LOGGER).type(PhotonLogTypes.WEB_SERVER).send();
        } catch (Exception e) {
            IEndpoint.handleException(AdminDeletePostEndpoint.class, handler, e);
        }
    }
}