package pingping.Youtube;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.tinylog.Logger;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import io.github.cdimascio.dotenv.Dotenv;

import com.google.api.client.json.gson.GsonFactory;


public class YoutubeAPI {
    // TODO tidy up and add error checking

    private static YouTube youtube = getYouTubeService();

    private static YouTube getYouTubeService() {
        try {
            return new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(), null)
                    .setApplicationName("PingPing")
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getChannelUploadsPlaylistId(String channelUserName) throws Exception {
        YouTube.Channels.List channelsList = youtube.channels().list("contentDetails");
        channelsList.setKey(Dotenv.load().get("YOUTUBE_API_KEY"));
        channelsList.setForUsername(channelUserName);

        ChannelListResponse response = channelsList.execute();
        Logger.debug(response.toPrettyString());
        if (response.getItems() != null && !response.getItems().isEmpty()) {
            Channel channel = response.getItems().get(0);
            System.out.println(channel.toPrettyString());
            return channel.getContentDetails().getRelatedPlaylists().getUploads();
        }
        return null;
    }

    public static String getLatestUploadVideoId(String playlistId) throws Exception {
        YouTube.PlaylistItems.List playlistItemsList = youtube.playlistItems().list("contentDetails");
        playlistItemsList.setKey(Dotenv.load().get("YOUTUBE_API_KEY"));
        playlistItemsList.setPlaylistId(playlistId);
        playlistItemsList.setMaxResults(1L);

        PlaylistItemListResponse response = playlistItemsList.execute();
        Logger.debug(response.toPrettyString());
        if (response.getItems() != null && !response.getItems().isEmpty()) {
            PlaylistItem playlistItem = response.getItems().get(0);
            return playlistItem.getContentDetails().getVideoId();
        }
        return null;
    }

    public static boolean isVideoLive(String videoId) throws Exception {
        YouTube.Videos.List videoList = youtube.videos().list("snippet,liveStreamingDetails");
        videoList.setKey(Dotenv.load().get("YOUTUBE_API_KEY"));
        videoList.setId(videoId);

        VideoListResponse response = videoList.execute();
        Logger.debug(response.toPrettyString());
        if (response != null && !response.getItems().isEmpty()) {
            Video video = response.getItems().get(0);
            return video.getSnippet().getLiveBroadcastContent().equals("live");
        }
        return false;
    }
}
