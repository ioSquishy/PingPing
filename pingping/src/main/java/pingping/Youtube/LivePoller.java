package pingping.Youtube;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.tinylog.Logger;

import com.google.api.services.youtube.model.Video;

import pingping.Database.Database;
import pingping.Database.OrmObjects.YoutubeChannel;
import pingping.Database.OrmObjects.YoutubeSub;
import pingping.Discord.Helpers.PushStreamNotification;
import pingping.Exceptions.DatabaseException;
import pingping.Exceptions.YoutubeApiException;

public class LivePoller {
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static Runnable pollApi() {
        return () -> {
            // pull youtube channels from database
            List<YoutubeChannel> channels;
            try {
                channels = Database.YoutubeChannelsTable.getAllChannelInfo();
                Logger.trace("Polling {} YoutubeChannels", channels.size());
            } catch (DatabaseException e) {
                Logger.error(e);
                return;
            }

            // TODO: make more quota efficient by bulk requesting Video objects
            // 1. poll API for updated latest video ids, add videoId's of updated ones to an array here
            // 2. use YoutubeAPI.getVideos(videoId's) and push notifs for each Video returned in that array
            //    ChannelId is included in the Video snippet to pull additional info from Database/API 

            // TODO maybe do this in a parallel stream in the future
            // poll whether streamer is live and push notifications if true
            for (YoutubeChannel dbChannelInfo : channels) {
                try {
                    Logger.trace("Polling live status for: {}", dbChannelInfo.broadcaster_handle);
                    // use YoutubeApi.getActiveLivestream
                    Video currentStream = YoutubeAPI.getActiveLivestream(dbChannelInfo.uploads_playlist_id).orElse(null);
                    if (currentStream == null) continue;

                    // if its active, make sure video id is not the same as last one stored in database
                    if (currentStream.getId().equals(dbChannelInfo.last_stream_video_id)) continue;

                    // check if broadcaster_handle is up-to-date
                    // TODO: to check broadcaster handle, need to call Channels: list API (it is under snippet.customUrl)
                    // ideally call for all channels in one big API call (to cost 1 quota) once per day or something

                    // store new last video id in database
                    Database.YoutubeChannelsTable.setLastStreamVideoId(dbChannelInfo.broadcaster_id, currentStream.getId());

                    // get channel profile picture
                    String pfpUrl = "https://developers.google.com/static/site-assets/logo-youtube.svg";
                    try {
                        pfpUrl = YoutubeAPI.getChannelFromId(dbChannelInfo.broadcaster_id).getSnippet().getThumbnails().getMedium().getUrl();
                    } catch (YoutubeApiException e) {
                        Logger.warn(e, "Failed to get profile picture for channel {}", dbChannelInfo.broadcaster_handle);
                    }

                    Logger.trace("Pushing stream notifications for: {}", dbChannelInfo.broadcaster_handle);
                    // get all subscriptions (for individual servers) with that broadcaster and push stream notification
                    List<YoutubeSub> subs = Database.YoutubeSubsTable.pullYoutubeSubsFromBroadcasterId(dbChannelInfo.broadcaster_id);
                    for (YoutubeSub sub : subs) {
                        PushStreamNotification.pushYoutubeStreamNotification(sub, currentStream, pfpUrl);
                    }
                    Logger.debug("Finished pushing stream notifications for: {}", dbChannelInfo.broadcaster_handle);
                } catch (DatabaseException | YoutubeApiException e) {
                    Logger.error(e, "Failed to poll livestream info for {}", dbChannelInfo.broadcaster_handle);
                }
            }
        };
    }

    public static void startPolling() {
        if (scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        
        scheduler.scheduleAtFixedRate(pollApi(), 0, 1L, TimeUnit.MINUTES);
        Logger.info("LivePoller Started.");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (scheduler != null) {
                try {
                    stopPolling();
                    System.out.println("LivePoller stopped by shutdown hook.");
                } catch (InterruptedException e) {
                    System.err.println("Error stopping LivePoller in shutdown hook.");
                    e.printStackTrace();
                }
            }
        }));
    }

    public static boolean stopPolling() throws InterruptedException {
        scheduler.shutdown();
        return scheduler.awaitTermination(5L, TimeUnit.SECONDS);
    }
}
