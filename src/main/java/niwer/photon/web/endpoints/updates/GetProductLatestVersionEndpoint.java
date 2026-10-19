package niwer.photon.web.endpoints.updates;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.Directories;
import niwer.photon.objects.ObjectGithubRelease;
import niwer.photon.objects.ObjectProduct;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.api.github.GetLatestReleaseRequest;
import niwer.photon.web.endpoints.IEndpoint;

/**
 * This allows to get the latest version of a product from GitHub. It requires the following query parameters:
 * - product: The ID of the product for which to get the latest version
 * 
 * @author Niwer
 */
public class GetProductLatestVersionEndpoint implements IEndpoint {

    @Override public String path() { return "/download/version"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context ctx) {
        IEndpoint.setupRateLimit(ctx, 10, TimeUnit.MINUTES);

        try {
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
    
            /* Get the latest version from GitHub tags */
            Optional<ObjectGithubRelease> request = new GetLatestReleaseRequest(PRODUCT).request();
            if(request.isEmpty()) {
                ctx.status(404).result("No published releases found for the requested product.");
                return;
            }
    
            final var LATEST_RELEASE = request.get();
            ctx.status(200).json(LATEST_RELEASE);
        } catch (Exception e) {
            IEndpoint.handleException(GetProductLatestVersionEndpoint.class, ctx, e);
        }
    }
}
