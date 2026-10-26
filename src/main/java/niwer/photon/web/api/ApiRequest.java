package niwer.photon.web.api;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import niwer.photon.util.GsonUtils;
import niwer.photon.web.HttpMethod;

/**
 * Abstract class representing a GitHub API request. This class provides methods to prepare the request and body, as well as to sanitize input strings.
 * 
 * @author Niwer
 */
public abstract class ApiRequest<T> {

    protected final transient HttpClient CLIENT;

    protected ApiRequest() {
        this(HttpClient.newHttpClient());
    }

    protected ApiRequest(HttpClient client) {
        this.CLIENT = Objects.requireNonNull(client, "client");
    }

    protected ApiRequest(HttpClient.Redirect redirect) {
        try {
            this.CLIENT = HttpClient.newBuilder().followRedirects(redirect).build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HTTP client", e);
        }
    }

    /**
     * Send the request to the server
     * 
     * @param <T> The type of the response expected from the server
     * @return An object representing the response from the server.
     * The type of the response is determined by the implementation of this method in subclasses.
     * It may also be null if the request fails or if the server returns an error response or if the request does not expect a response body.
     */
    public abstract T request();

    /**
     * Get the URL of the request
     * 
     * @return The URL of the request
     */
    public abstract String url();

    /**
     * Get the HTTP method of the request
     * 
     * @return The HTTP method of the request
     */
    public abstract HttpMethod method();

    /**
     * Add necessary headers to the HttpRequest.Builder. This method should be implemented by subclasses to add any specific headers required for the request.
     * 
     * @param builder The HttpRequest.Builder to which headers should be added
     */
    public abstract void addHeaders(HttpRequest.Builder builder);
    
    private final URI toURI() { return URI.create(url()); }

    private final HttpRequest.BodyPublisher prepareBody() {
        return HttpRequest.BodyPublishers.ofString(GsonUtils.GSON.toJson(this));
    }

    private final HttpRequest asRequest() {
        final HttpRequest.Builder builder = HttpRequest.newBuilder().uri(this.toURI()).header("User-Agent", "Photon-Backend");
        this.addHeaders(builder); // Add any additional headers defined in the subclass
        
        switch (this.method()) {
            case GET -> builder.GET();
            case POST -> builder.POST(this.prepareBody());
            case PUT -> builder.PUT(this.prepareBody());
            case DELETE -> builder.DELETE();
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + this.method());
        }

        return builder.build();
    }

    /**
     * Send the HTTP request and return the response. This method uses the HttpClient to send the request prepared by the asRequest() method and returns the HttpResponse.
     * 
     * @param <E> The type of the response body
     * @param bodyHandler The handler for processing the response body
     * @return The HTTP response
     * @throws Exception If an error occurs while sending the request
     */
    protected final <E> HttpResponse<E> sendHttpRequest(HttpResponse.BodyHandler<E> bodyHandler) throws Exception {
        return this.CLIENT.send(this.asRequest(), bodyHandler);
    }

    /**
     * Send the HTTP request asynchronously and return the response. This method uses the HttpClient to send the request prepared by the asRequest() method and returns the HttpResponse.
     * 
     * @param <E> The type of the response body
     * @param bodyHandler The handler for processing the response body
     * @return The HTTP response
     * @throws Exception If an error occurs while sending the request
     */
    protected final <E> HttpResponse<E> sendHttpRequestAsync(HttpResponse.BodyHandler<E> bodyHandler) throws Exception {
        return this.CLIENT.sendAsync(this.asRequest(), bodyHandler).join();
    }

    /**
     * Sanitize a string by removing all non-alphanumeric characters and converting it to lowercase
     * 
     * @param input The string to sanitize
     * @return The sanitized string
     */
    protected String sanitizeAndLower(String input) { return input.replaceAll("[^a-zA-Z0-9-_]", "").toLowerCase(); }

    /**
     * URL-encode a string using UTF-8 encoding. This method is useful for encoding query parameters or path segments in URLs.
     * 
     * @param value The string to URL-encode
     * @return The URL-encoded string
     */
    protected String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
