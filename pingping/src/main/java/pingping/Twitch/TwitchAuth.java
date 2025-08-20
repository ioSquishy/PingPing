package pingping.Twitch;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.cdimascio.dotenv.Dotenv;
import pingping.Http;

public class TwitchAuth {
    private static final String tokenRequestUrl = "https://id.twitch.tv/oauth2/token";
    private static final String tokenRequestBody = 
        "client_id=" + Dotenv.load().get("TWITCH_CLIENT_ID") +
        "&client_secret=" + Dotenv.load().get("TWITCH_SECRET") +
        "&grant_type=client_credentials";

    private static final Callable<HttpResponse<String>> requestNewToken = Http.createRequest(Http.POST(tokenRequestUrl, Map.of(), tokenRequestBody));
    public static String appAccessToken = "";
    private static int tokenTtl = -1;

    private static ScheduledExecutorService reRequestExe = Executors.newSingleThreadScheduledExecutor();
    private static final Runnable refreshTokenRunnable = () -> {
        refreshToken();
    };
    

    public static boolean InitTokenRequests() {
        return refreshToken();
    }

    private static boolean refreshToken() {
        HttpResponse<String> requestResponse;
        try {
            requestResponse = requestNewToken.call();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        if (requestResponse.statusCode() == 200) {
            boolean parseSuccess = ParseJson(requestResponse.body());
            if (parseSuccess) {
                reRequestExe.schedule(refreshTokenRunnable, tokenTtl, TimeUnit.SECONDS);
                System.out.println("Twitch token retrieved!");
                return true;
            } else {
                reRequestExe.schedule(refreshTokenRunnable, 5, TimeUnit.SECONDS);
                System.err.println("Twitch token request FAILED: " + requestResponse.body());
                return false;
            }
        } else {
            System.err.println("Twitch token request FAILED: " + requestResponse.body());
            return false;
        }
    }

    private static ObjectMapper mapper = new ObjectMapper();
    private static boolean ParseJson(String json) {
        JsonNode rootNode;
        try {
            rootNode = mapper.readTree(json);
            appAccessToken = rootNode.path("access_token").asText();
            tokenTtl = rootNode.path("expires_in").asInt();
            return true;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return false;
        }
    }
}
