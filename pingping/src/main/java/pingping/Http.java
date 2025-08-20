package pingping;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Basic abstraction of Http request methods for use with JSON inputs.
 * Good tutorial: https://youtu.be/9oq7Y8n1t00?si=t60rcjT-dZAJmqQA
 */
public class Http {

    private static final HttpClient client = HttpClient.newHttpClient();

    private Http() {}
    
    public static HttpRequest POST(String uri, Map<String, String> headers, String jsonBody) {
        try {
            HttpRequest.Builder request = buildRequestHead(uri, headers);
            request.POST(BodyPublishers.ofString(jsonBody));
            return request.build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static HttpRequest GET(String uri, Map<String, String> headers) {
        try {
            HttpRequest.Builder request = buildRequestHead(uri, headers);
            request.GET();
            return request.build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Callable<HttpResponse<String>> createRequest(HttpRequest request) {
        return new CallableRequest(request);
    }

    private static class CallableRequest implements Callable<HttpResponse<String>> {
        public final HttpRequest request;

        public CallableRequest(HttpRequest request) {
            this.request = request;
        }

        @Override
        public HttpResponse<String> call() throws Exception {
            return Http.getResponse(request);
        }
    }

    /**
     * Returns the body of an HttpResponse from an HttpRequest sent using HttpClient
     * @param request HttpRequest to send and return response of
     * @return body of the HttpResponse
     */
    private static HttpResponse<String> getResponse(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, BodyHandlers.ofString());
    }

    /**
     * Returns an HttpRequest Builder with the URI and Headers added.
     * @param uri uri of HttpRequest to build
     * @param headers headers of HttpRequest to build
     * @return HttpRequest Builder with the URI and Headers added
     * @throws URISyntaxException
     * @throws IllegalArgumentException
     */
    private static HttpRequest.Builder buildRequestHead(String uri, Map<String, String> headers) throws URISyntaxException, IllegalArgumentException {
        HttpRequest.Builder request = HttpRequest.newBuilder();
        request.uri(new URI(uri));
        headers.forEach(request::header);
        return request;
    }

    /**
     * Exception class
     */
    public class HttpException extends RuntimeException {
        public HttpException() {
            super();
        }
    
        public HttpException(String message) {
            super(message);
        }
    }
}