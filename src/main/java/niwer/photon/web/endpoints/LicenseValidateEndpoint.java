package niwer.photon.web.endpoints;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.util.license.LicenseManager;
import niwer.photon.util.license.LicenseValidationResult;
import niwer.photon.web.HttpMethod;

/**
 * Endpoint for validating a license key against a product ID and hardware ID.
 * It checks if the provided license key is valid for the specified product and hardware, returning the validation result along with any associated claims.
 */
public class LicenseValidateEndpoint implements IEndpoint {

    @Override public String path() { return "/licenses/validate"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 15, TimeUnit.SECONDS);
        
        try {
            /* Get the license key */
            final Map<String, Object> BODY = EndpointUtils.parseBody(handler.body());
            final String LICENSE_KEY = EndpointUtils.getString(handler, BODY, "license_key");
            if (LICENSE_KEY == null || LICENSE_KEY.isBlank()) {
                handler.status(400).result("Missing license key");
                return;
            }
    
            /* Get the product ID */
            final String EXPECTED_PRODUCT_ID = EndpointUtils.getString(handler, BODY, "product_id");
            if (EXPECTED_PRODUCT_ID == null || EXPECTED_PRODUCT_ID.isBlank()) {
                handler.status(400).result("Missing product id");
                return;
            }
    
            /* Get the hardware ID */
            final String HARDWARE_ID = EndpointUtils.getString(handler, BODY, "hardware_id", "hardwareId", "hwid");
            if (HARDWARE_ID == null || HARDWARE_ID.isBlank()) {
                handler.status(400).result("Missing hwid");
                return;
            }
    
            /* Validate the license */
            final LicenseValidationResult result = LicenseManager.validateLicense(LICENSE_KEY, EXPECTED_PRODUCT_ID, HARDWARE_ID);
            handler.status(200).json(result.payload());
        } catch (Exception e) {
            IEndpoint.handleException(LicenseValidateEndpoint.class, handler, e);
        }
    }
}
