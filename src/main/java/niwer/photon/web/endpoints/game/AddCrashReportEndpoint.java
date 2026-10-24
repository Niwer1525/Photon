package niwer.photon.web.endpoints.game;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.sql.CrashReportTable;
import niwer.photon.sql.CrashReportTable.CrashReportSides;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class AddCrashReportEndpoint implements IEndpoint {

    @Override public String path() { return "/game/add-crash-report"; }

    @Override public HttpMethod method() { return HttpMethod.POST; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 10, TimeUnit.MINUTES);

        try {
            final String FILE_MESSAGE = handler.formParam("fileMessage");
            final String USER_UUID = handler.formParam("userUUID");
            final String SIDE = handler.formParam("side");
            // final String OPERATRING_SYSTEM = handler.formParam("operatingSystem"); // Not used right now, but can be useful
            final String TIME_STAMP = handler.formParam("timestamp");
    
            /* Ensure all parameters are provided */
            if (FILE_MESSAGE == null || TIME_STAMP == null || USER_UUID == null) {
                handler.status(HttpStatus.BAD_REQUEST).result("Missing parameters");
                return;
            }
    
            /* Ensure parameters are not blank */
            if (FILE_MESSAGE.isBlank() || TIME_STAMP.isBlank() || USER_UUID.isBlank() || (SIDE != null && SIDE.isBlank())) {
                handler.status(HttpStatus.BAD_REQUEST).result("Parameters cannot be blank");
                return;
            }
    
            final CrashReportSides reportSide = SIDE == null ? CrashReportSides.CLIENT : CrashReportSides.fromString(SIDE);
            CrashReportTable.save(USER_UUID, TIME_STAMP, FILE_MESSAGE, reportSide);
            Console.log("Crash report received: " + USER_UUID).sendToProcessor().type(PhotonLogTypes.NETWORK).container(PhotonEngine.LOGGER).send();
            handler.status(HttpStatus.OK).result("Crash report received");
        } catch (Exception e) {
            IEndpoint.handleException(AddCrashReportEndpoint.class, handler, e);
        }
    }
}