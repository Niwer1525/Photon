package niwer.photon.web.endpoints.game;

import java.util.concurrent.TimeUnit;

import io.javalin.http.Context;
import niwer.photon.Directories;
import niwer.photon.Directories.NetworkConfig;
import niwer.photon.web.HttpMethod;
import niwer.photon.web.endpoints.IEndpoint;

public class InfoEndpoint implements IEndpoint {

    @Override public String path() { return "/game/config"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override
    public void handle(Context handler) {
        IEndpoint.setupRateLimit(handler, 5, TimeUnit.MINUTES);
        
        try {
            final NetworkConfig CONFIG = Directories.getConfig();
            handler.json(new Config(CONFIG));
        } catch (Exception e) {
            IEndpoint.handleException(InfoEndpoint.class, handler, e);
        }
    }

    private record Config(
        String official_logo_base64,
        String discord_bot_id,

        String twitter_url,
        String twitch_url,
        String youtube_url,
        String discord_url,
        String website_url
    ) {
        public Config(NetworkConfig config) {
            this(
                Directories.getOfficialLogoBase64(),
                config.discord_bot_id,
                
                config.twitter_url,
                config.twitch_url,
                config.youtube_url,
                config.discord_url,
                config.website_url
            );
        }
    }
}