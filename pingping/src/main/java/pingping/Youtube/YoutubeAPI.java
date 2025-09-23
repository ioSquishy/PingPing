package pingping.Youtube;

public class YoutubeAPI {
    /*
     * // TODO have to create a method to poll the youtube api
     *  Retrieve recent videos from a channel: https://developers.google.com/youtube/v3/docs/channels/list?hl=en
            Every channel has an "uploads" playlist that is automatically updated with its newest videos. You can get the playlistId for this list from the channels.list endpoint. A single channels.list call costs only 1 unit of quota.
        Retrieve playlist items: https://developers.google.com/youtube/v3/docs/playlistItems/list?hl=en
            Call the playlistItems.list endpoint with the channel's "uploads" playlistId. This call costs only 1 unit and returns the most recent videos uploaded by the channel.
        Check video status for recently uploaded videos: https://developers.google.com/youtube/v3/docs/videos/list?hl=en
            For the video IDs returned by the playlistItems.list call, use the videos.list endpoint to retrieve details. This call costs 1 unit per video. You can send up to 50 video IDs in a single videos.list request by using a comma-separated list. 
     */
}
