package niwer.photon.web.endpoints.game;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.Directories;
import niwer.photon.PhotonConfig;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class InfoEndpoint implements IEndpoint {

    @Override public String path() { return "/game/config"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);
        
        try {
            final PhotonConfig CONFIG = Directories.config();
            handler.json(new Config(CONFIG));
        } catch (Exception e) {
            IEndpoint.handleException(InfoEndpoint.class, handler, e);
        }
    }

    private record Config(
        String official_logo_base64,
        String discord_bot_id
    ) {
        public Config(PhotonConfig config) {
            this(
                Directories.getOfficialLogoBase64(),
                config.discord_bot_id
            );
        }
    }
}