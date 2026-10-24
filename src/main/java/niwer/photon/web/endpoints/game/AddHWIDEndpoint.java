package niwer.photon.web.endpoints.game;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.sql.HWIDTable;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AddHWIDEndpoint implements IEndpoint {

    @Override public String path() { return "/game/add-hwid"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 10, TimeUnit.MINUTES);

        try {
            final String HWID = handler.formParam("hwid");
            final String USER_UUID = handler.formParam("userUUID");
            final String OPERATRING_SYSTEM = handler.formParam("operatingSystem");
    
            /* Ensure all parameters are provided */
            if (HWID == null || USER_UUID == null || OPERATRING_SYSTEM == null) {
                handler.status(HttpStatus.BAD_REQUEST).result("Missing parameters");
                return;
            }
    
            /* Ensure parameters are not blank */
            if (HWID.isBlank() || USER_UUID.isBlank() || OPERATRING_SYSTEM.isBlank()) {
                handler.status(HttpStatus.BAD_REQUEST).result("Parameters cannot be blank");
                return;
            }
    
            HWIDTable.save(USER_UUID, HWID, OPERATRING_SYSTEM);
            Console.log("Saving new HWID entry for user: " + USER_UUID).sendToProcessor().type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
            handler.status(HttpStatus.OK).result("HWID entry saved successfully");
        } catch (Exception e) {
            IEndpoint.handleException(AddHWIDEndpoint.class, handler, e);
        }
    }
}