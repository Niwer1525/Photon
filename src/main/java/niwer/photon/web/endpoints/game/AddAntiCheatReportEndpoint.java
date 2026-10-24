package niwer.photon.web.endpoints.game;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.sql.AnticheatTable;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AddAntiCheatReportEndpoint implements IEndpoint {

    @Override public String path() { return "/game/add-anticheat-report"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 10, TimeUnit.MINUTES);
        
        try {
            final String FILE_MESSAGE = handler.formParam("fileMessage");
            final String USER_UUID = handler.formParam("userUUID");
            final String OPERATRING_SYSTEM = handler.formParam("operatingSystem");
            final String TIME_STAMP = handler.formParam("timestamp");
    
            /* Ensure all parameters are provided */
            if (FILE_MESSAGE == null || TIME_STAMP == null || USER_UUID == null || OPERATRING_SYSTEM == null) {
                handler.status(HttpStatus.BAD_REQUEST).result("Missing parameters");
                return;
            }
    
            /* Ensure parameters are not blank */
            if (FILE_MESSAGE.isBlank() || TIME_STAMP.isBlank() || USER_UUID.isBlank() || OPERATRING_SYSTEM.isBlank()) {
                handler.status(HttpStatus.BAD_REQUEST).result("Parameters cannot be blank");
                return;
            }
    
            AnticheatTable.save(USER_UUID, TIME_STAMP, FILE_MESSAGE, OPERATRING_SYSTEM);
            Console.log("Anticheat report received: " + USER_UUID).sendToProcessor().type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
            handler.status(HttpStatus.OK).result("Anticheat report received successfully");
        } catch (Exception e) {
            IEndpoint.handleException(AddAntiCheatReportEndpoint.class, handler, e);
        }
    }
}