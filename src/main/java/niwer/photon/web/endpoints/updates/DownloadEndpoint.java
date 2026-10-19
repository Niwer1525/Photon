package niwer.photon.web.endpoints.updates;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.Directories;
import niwer.photon.objects.ObjectProduct;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.api.github.DownloadAssetService;
import niwer.photon.web.endpoints.IEndpoint;

/**
 * Serves mod update files from GitHub releases.
 * TODO : In the future, this should be replaced by modrinth for all projects that are hosted there.
 * 
 * @author Niwer
 */
public class DownloadEndpoint implements IEndpoint {

    @Override public String path() { return "/download"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context ctx) {
        IEndpoint.setupRateLimit(ctx, 10, TimeUnit.MINUTES);

        try {
            /* Check if any asset id was provided as query parameter */
            final var ASSET_NAME = ctx.queryParam("assetName");
            if (ASSET_NAME == null) {
                ctx.status(400).result("Please provide the asset name.");
                return;
            }
    
            /* Check if any asset id was provided as query parameter */
            final var ASSET_ID = ctx.queryParam("assetId");
            if (ASSET_ID == null) {
                ctx.status(400).result("Please provide the asset ID.");
                return;
            }
    
            /* Check if any repo was provided as query parameter */
            final var PRODUCT_ID = ctx.queryParam("product");
            if(PRODUCT_ID == null) {
                ctx.status(400).result("Please provide a valid product ID.");
                return;
            }
    
            final ObjectProduct PRODUCT = Directories.getConfig().getProducts().stream()
                .filter(product -> PRODUCT_ID.equals(product.id()) || (product.hasRepo() && PRODUCT_ID.equals(product.repoKey())))
                .findFirst().orElse(null);
            final var REPO = PRODUCT.repoName();
            final var OWNER = PRODUCT.repoOwner();
            if (REPO == null || OWNER == null) {
                ctx.status(400).result("The requested product is not available.");
                return;
            }
    
            /* Get the latest version from GitHub */
            final long ASSET_NUMBER;
            try {
                ASSET_NUMBER = Long.parseLong(ASSET_ID);
            } catch (NumberFormatException e) {
                ctx.status(400).result("The asset ID must be a number.");
                return;
            }
    
            final InputStream MOD_FILE = new DownloadAssetService(OWNER, REPO, ASSET_NUMBER).request();
            if (MOD_FILE == null) {
                ctx.status(404).result("The requested asset could not be downloaded.");
                return;
            }
    
            ctx.contentType("application/java-archive"); // Set the MIME type 
            ctx.header("Content-Disposition", "attachment; filename=\"" + ASSET_NAME + "\""); // Tell the browser to treat it as a downloadable attachment with a filename
            ctx.status(200);
            ctx.result(MOD_FILE); // pass the input stream directly
        } catch (Exception e) {
            IEndpoint.handleException(DownloadEndpoint.class, ctx, e);
        }
    }
}
