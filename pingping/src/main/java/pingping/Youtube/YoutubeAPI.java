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
    private static final String YOUTUBE_API_KEY = Dotenv.load().get("YOUTUBE_API_KEY");

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
     * Returns a channel from Handle
     * @param channelHandle
     * @return Channel object of channel
     * @throws YoutubeApiException if channel not found or IO exception
     */
    public static Channel getChannelFromHandle(String channelHandle) throws YoutubeApiException {
        try {
            YouTube.Channels.List channelsList = getYouTubeService().channels().list("contentDetails");
            channelsList.setKey(YOUTUBE_API_KEY);
            channelsList.set("forHandle", channelHandle);

            return getChannel(channelsList);
        } catch (Exception e) {
            Logger.error(e);
            throw new YoutubeApiException("Failed to get channel.");
        }
    }

    /**
     * Returns a channel from ID
     * @param channelHandle
     * @return Channel object of channel
     * @throws YoutubeApiException if channel not found or IO exception
     */
    public static Channel getChannelFromId(String id) throws YoutubeApiException {
        try {
            YouTube.Channels.List channelsList = getYouTubeService().channels().list("contentDetails,snippet");
            channelsList.setKey(YOUTUBE_API_KEY);
            channelsList.set("id", id);

            return getChannel(channelsList);
        } catch (Exception e) {
            Logger.error(e);
            throw new YoutubeApiException("Failed to get channel.");
        }
    }

    private static Channel getChannel(YouTube.Channels.List request) throws YoutubeApiException {
        try {
            ChannelListResponse response = request.execute();

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
            playlistItemsList.setKey(YOUTUBE_API_KEY);
            playlistItemsList.setPlaylistId(playlistId);
            playlistItemsList.setMaxResults(1L);

            PlaylistItemListResponse response = playlistItemsList.execute();
            

            if (response.getItems() != null && !response.getItems().isEmpty()) {
                PlaylistItem playlistItem = response.getItems().get(0);
                Logger.trace("Got latest video id ({}) from playlistId ({})", playlistItem.getContentDetails().getVideoId(), playlistId);
                return playlistItem.getContentDetails().getVideoId();
            } else {
                Logger.trace(response.toPrettyString());
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
            videoList.setKey(YOUTUBE_API_KEY);
            videoList.setId(videoId);

            VideoListResponse response = videoList.execute();
            
            if (response != null && !response.getItems().isEmpty()) {
                Video video = response.getItems().get(0);
                Logger.trace("Got video object for videoId: {}", videoId);
                return video;
            } else {
                if (response != null) {
                    Logger.trace(response.toPrettyString());
                } else {
                    Logger.warn("Response for video from video id was null.");
                }
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
