package pingping.Twitch;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.tinylog.Logger;

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
    protected static String appAccessToken = "";

    protected static boolean refreshAppAccessToken() {
        HttpResponse<String> requestResponse;
        try {
            requestResponse = requestNewToken.call();
        } catch (Exception e) {
            Logger.error(e);
            return false;
        }
        
        if (requestResponse.statusCode() == 200) {
            boolean parseSuccess = ParseJson(requestResponse.body());
            if (parseSuccess) {
                Logger.debug("Twitch token retrieved!");
                return true;
            } else {
                Logger.error("Twitch token request FAILED: {}", requestResponse.body());
                return false;
            }
        } else {
            Logger.error("Twitch token request FAILED: {}", requestResponse.body());
            return false;
        }
    }

    private static ObjectMapper mapper = new ObjectMapper();
    private static boolean ParseJson(String json) {
        try {
            JsonNode rootNode = mapper.readTree(json);
            appAccessToken = rootNode.path("access_token").asText();
            return true;
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }
}