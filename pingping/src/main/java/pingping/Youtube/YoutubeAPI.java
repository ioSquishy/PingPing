package pingping.Youtube;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

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
import pingping.Exceptions.YoutubeApiException;

import com.google.api.client.json.gson.GsonFactory;


public class YoutubeAPI {
    private static YouTube youtube = null;

    private static YouTube getYouTubeService() {
        if (youtube == null) {
            try {
                youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), null)
                    .setApplicationName("PingPing")
                    .build();
            } catch (GeneralSecurityException | IOException e) {
                Logger.error(e, "Failed to create youtube service instance.");
                return null;
            }
        }
        return youtube;
    }

    /**
     * @param uploadsPlaylistId
     * @return the video if they are live, or else an empty optional
     * @throws YoutubeApiException
     */
    public static Optional<Video> getActiveLivestream(String uploadsPlaylistId) throws YoutubeApiException {
        String latestUploadId = getLatestUploadVideoId(uploadsPlaylistId);
        Video latestVideo = getVideo(latestUploadId);
        if (isVideoLive(latestVideo)) {
            return Optional.of(latestVideo);
        } else {
            return Optional.empty();
        }
    }

    /**
     * 
     * @param channelHandle
     * @return Channel object of channel
     * @throws YoutubeApiException if channel not found or IO exception
     */
    public static Channel getChannel(String channelHandle) throws YoutubeApiException {
        try {
            YouTube.Channels.List channelsList = getYouTubeService().channels().list("contentDetails");
            channelsList.setKey(Dotenv.load().get("YOUTUBE_API_KEY"));
            channelsList.set("forHandle", channelHandle);

            ChannelListResponse response = channelsList.execute();
            Logger.trace(response.toPrettyString());

            if (response.getItems() != null && !response.getItems().isEmpty()) {
                Channel channel = response.getItems().get(0);
                return channel;
            } else {
                throw new YoutubeApiException("Channel not found.");
            }
        } catch (Exception e) {
            Logger.error(e);
            throw new YoutubeApiException("Failed to get channel.");
        }
    }

    public static String getChannelUploadsPlaylistId(Channel channel) throws YoutubeApiException {
        try {
            return channel.getContentDetails().getRelatedPlaylists().getUploads();
        } catch (NullPointerException e) {
            Logger.error(e);
            throw new YoutubeApiException("Failed to get uploads playlist id.");
        }
    }

    public static String getChannelUploadsPlaylistId(String channelHandle) throws YoutubeApiException {
        Channel channel = getChannel(channelHandle);
        return getChannelUploadsPlaylistId(channel);
    }

    public static String getChannelId(Channel channel) throws YoutubeApiException {
        try {
            return channel.getId();
        } catch (NullPointerException e) {
            Logger.error(e);
            throw new YoutubeApiException("Failed to get channel id.");
        }
    }

    public static String getLatestUploadVideoId(String playlistId) throws YoutubeApiException {
        try {
            YouTube.PlaylistItems.List playlistItemsList = getYouTubeService().playlistItems().list("contentDetails");
            playlistItemsList.setKey(Dotenv.load().get("YOUTUBE_API_KEY"));
            playlistItemsList.setPlaylistId(playlistId);
            playlistItemsList.setMaxResults(1L);

            PlaylistItemListResponse response = playlistItemsList.execute();
            Logger.trace(response.toPrettyString());

            if (response.getItems() != null && !response.getItems().isEmpty()) {
                PlaylistItem playlistItem = response.getItems().get(0);
                return playlistItem.getContentDetails().getVideoId();
            } else {
                throw new YoutubeApiException("No videos found for playlist id: " + playlistId);
            }
        } catch (Exception e) {
            Logger.error(e);
            throw new YoutubeApiException("Failed to get latest upload video id.");
        }
    }

    public static Video getVideo(String videoId) throws YoutubeApiException {
        try {
            YouTube.Videos.List videoList = getYouTubeService().videos().list("snippet,liveStreamingDetails");
            videoList.setKey(Dotenv.load().get("YOUTUBE_API_KEY"));
            videoList.setId(videoId);

            VideoListResponse response = videoList.execute();
            Logger.trace(response.toPrettyString());
            if (response != null && !response.getItems().isEmpty()) {
                Video video = response.getItems().get(0);
                return video;
            } else {
                throw new YoutubeApiException("No videos found for videoId: " + videoId);
            }
        } catch (Exception e) {
            Logger.error(e);
            throw new YoutubeApiException("Failed to get video.");
        }
    }

    public static boolean isVideoLive(Video video) {
        try {
            String liveBroadcastContent = video.getSnippet().getLiveBroadcastContent();
            return liveBroadcastContent != null && liveBroadcastContent.equals("live");
        } catch (NullPointerException e) {
            Logger.error(e);
            return false;
        }
    }
}
