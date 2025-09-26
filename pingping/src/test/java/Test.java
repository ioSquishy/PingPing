import pingping.Youtube.YoutubeAPI;

public class Test {
    public static void main(String[] args) {
        try {
            YoutubeAPI.connectYoutubeApi();
            YoutubeAPI.getActiveLivestream("UUIeSUTOTkF9Hs7q3SGcO");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
