import com.google.api.services.youtube.model.Video;

import pingping.Youtube.YoutubeAPI;

public class Test {
    public static void main(String[] args) {
        try {
            Video video = YoutubeAPI.getActiveLivestream("Sykkuno").get();
            System.out.println(video.toPrettyString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
