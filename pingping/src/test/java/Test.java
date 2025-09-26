import pingping.Youtube.YoutubeAPI;

public class Test {
    public static void main(String[] args) {
        try {
            String pid = YoutubeAPI.getChannelUploadsPlaylistId("Sykkuno");
            System.out.println("pid: " + pid);
            String vid = YoutubeAPI.getLatestUploadVideoId(pid);
            System.out.println("vid: " + vid);
            boolean live = YoutubeAPI.isVideoLive(vid);
            System.out.println("live: " + live);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
