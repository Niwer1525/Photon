package niwer.photon.web.endpoints;

import java.util.concurrent.TimeUnit;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.plugin.bundled.RateLimitPlugin;
import niwer.lumen.Console;
import niwer.photon.PhotonEngine;
import niwer.photon.util.PhotonLogTypes;
import niwer.photon.web.HttpMethod;

/**
 * Interface representing an API endpoint in the application. Each endpoint must specify its path, HTTP method, and handler function.
 * 
 * @author Niwer
 */
public interface IEndpoint {
    /**
     * Returns the path for this endpoint (e.g., "/api/auth/sign-up"). This path is used to route incoming requests to the appropriate handler.
     * 
     * @return The path for this endpoint
     */
    String path();
    
    /**
     * Returns the HTTP method (GET, POST, PUT, DELETE) that this endpoint responds to.
     * 
     * @return The HTTP method for this endpoint
     */
    HttpMethod method();

    /**
     * Handles incoming requests to this endpoint. The implementation should process the request and set the appropriate response on the provided Context object.
     * 
     * @param handler The Javalin Context object representing the incoming request and response. The handler should use this object to read request data and set the response.
     */
    void handle(Context handler);

    /**
     * Sets up a rate limit for the given handler.
     * This method configures the RateLimitPlugin to allow a specified number of requests per time unit.
     * 
     * @param handler The Javalin Context object representing the incoming request and response.
     * @param numRequests The maximum number of requests allowed within the specified time unit.
     * @param unit The time unit for the rate limit (e.g., TimeUnit.MINUTES, TimeUnit.SECONDS).
     */
    public static void setupRateLimit(Context handler, int numRequests, TimeUnit unit) {
        handler.with(RateLimitPlugin.class).requestPerTimeUnit(numRequests, unit);
    }

    /**
     * Registers the given endpoint class with the provided JavalinConfig. The endpoint class must have a no-argument constructor.
     * 
     * @param cfg The JavalinConfig to register the endpoint with
     * @param endpointClass The class of the endpoint to register
     * @return The instance of the registered endpoint
     */
    public static IEndpoint register(JavalinConfig cfg, Class<? extends IEndpoint> endpointClass) {
        try {
            final IEndpoint END_POINT = endpointClass.getDeclaredConstructor().newInstance();
            switch (END_POINT.method()) {
                case GET -> cfg.routes.get(END_POINT.path(), END_POINT::handle);
                case POST -> cfg.routes.post(END_POINT.path(), END_POINT::handle);
                case PUT -> cfg.routes.put(END_POINT.path(), END_POINT::handle);
                case DELETE -> cfg.routes.delete(END_POINT.path(), END_POINT::handle);
            }
            return END_POINT;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate endpoint: " + endpointClass.getName(), e);
        }
    }

    /**
     * Handles exceptions that occur during the processing of an endpoint. Logs the error and sends a 500 Internal Server Error response to the client.
     * 
     * @param endpointClass The class of the endpoint where the exception occurred
     * @param handler The Javalin Context object representing the incoming request and response
     * @param e The exception that occurred during endpoint processing
     */
    public static void handleException(Class<? extends IEndpoint> endpointClass, Context handler, Exception e) {
        Console.log("Error in " + endpointClass.getSimpleName(), e).error().type(PhotonLogTypes.WEB_SERVER).container(PhotonEngine.LOGGER).send();
        handler.status(500).result("Internal server error: " + e.getMessage());
    }
}