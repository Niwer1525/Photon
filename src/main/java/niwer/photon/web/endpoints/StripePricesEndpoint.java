package niwer.photon.web.endpoints;

import java.util.concurrent.TimeUnit;

import com.stripe.Stripe;
import com.stripe.model.Price;
import com.stripe.param.PriceListParams;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.photon.Directories;
import niwer.photon.web.HttpMethod;

/**
 * This endpoint is used to fetch the list of active prices from Stripe.
 * 
 * @author Niwer
 */
public class StripePricesEndpoint implements IEndpoint {

    @Override public String path() { return "/stripe_prices"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 15, TimeUnit.MINUTES);

        try {
            final String API_KEY = Directories.config().stripe_api_key;
            if (API_KEY == null || API_KEY.isBlank()) {
                handler.status(HttpStatus.INTERNAL_SERVER_ERROR).result("stripe_api_key is not configured");
                return;
            }
            Stripe.apiKey = API_KEY;

            final PriceListParams PARAMS = PriceListParams.builder()
                .setActive(true)
                .addExpand("data.product")
                .build();
            handler.status(HttpStatus.OK)
                .contentType("application/json")
                .result(Price.list(PARAMS).toJson());
        } catch (Exception e) {
            IEndpoint.handleException(StripePricesEndpoint.class, handler, e);
        }
    }
}
