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
            } catch (DatabaseException e) {
                Logger.error(e);
                return;
            }

            // poll whether stream subscription needs to be sent for all channels in database
            for (YoutubeChannel dbChannelInfo : channels) {
                try {
                    // use YoutubeApi.getActiveLivestream
                    Video currentStream = YoutubeAPI.getActiveLivestream(dbChannelInfo.uploads_playlist_id).orElse(null);
                    if (currentStream == null) continue;

                    // if its active, make sure video id is not the same as last one stored in database
                    if (currentStream.getId() == dbChannelInfo.last_stream_video_id) continue;

                    // check if broadcaster_handle is up-to-date
                    if (currentStream.getSnippet().getChannelTitle() != dbChannelInfo.broadcaster_handle) {
                        // update broadcaster handle
                        Database.YoutubeChannelsTable.setBroadcasterHandle(dbChannelInfo.broadcaster_handle, currentStream.getSnippet().getChannelTitle());
                    }

                    // store new last video id in database
                    Database.YoutubeChannelsTable.setLastStreamVideoId(dbChannelInfo.broadcaster_id, currentStream.getId());

                    // get all subscriptions (for individual servers) with that broadcaster and push stream notification
                    List<YoutubeSub> subs = Database.YoutubeSubsTable.pullYoutubeSubsFromBroadcasterId(dbChannelInfo.broadcaster_id);
                    for (YoutubeSub sub : subs) {
                        PushStreamNotification.pushYoutubeStreamNotification(sub);
                    }
                } catch (DatabaseException | YoutubeApiException e) {
                    Logger.error(e);
                }
            }
        };
    }

    public static void startPolling() {
        if (scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        
        scheduler.scheduleAtFixedRate(pollApi(), 0, 1L, TimeUnit.MINUTES);

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
