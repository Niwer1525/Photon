package niwer.photon.web.endpoints;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import niwer.photon.Directories;
import niwer.photon.web.HttpMethod;

public class LogoEndpoint implements IEndpoint {

    @Override public String path() { return "/photon_logo"; }

    @Override public HttpMethod method() { return HttpMethod.GET; }

    @Override public void handle(Context handler) {
        final BufferedImage LOGO_BASE64 = Directories.getOfficialLogoImage();
        if (LOGO_BASE64 == null) {
            handler.status(HttpStatus.NOT_FOUND).result("Logo not found");
            return;
        }

        final var OUTPUT_STREAM = new ByteArrayOutputStream();
        try {
            ImageIO.write(LOGO_BASE64, "png", OUTPUT_STREAM);
        } catch (Exception e) {
            handler.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Error processing logo image");
            return;
        }

        handler.contentType("image/png");
        handler.header("Cache-Control", "public, max-age=31536000"); // Cache for 1 year
        handler.header("Expires", "Thu, 31 Dec 2037 23:59:59 GMT"); // Set an expiration date far in the future
        handler.header("ETag", "photon_logo_v1"); // Set a static ETag for the logo
        handler.status(HttpStatus.OK).result(OUTPUT_STREAM.toByteArray());
    }
}