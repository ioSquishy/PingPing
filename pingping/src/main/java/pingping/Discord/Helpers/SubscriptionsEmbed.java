package pingping.Discord.Helpers;

import java.util.ArrayList;
import java.util.List;
import java.awt.Color;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.tinylog.Logger;

import pingping.Database.OrmObjects.StreamerSubscription;
import pingping.Discord.Constants;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Exceptions.TwitchApiException;
import pingping.Twitch.TwitchAPI;

public class SubscriptionsEmbed {
    /**
     * returns a list of embeds needed to show all subscriptions for a server
     * @param subscriptions
     */
    public static List<EmbedBuilder> embedSubscriptions(List<? extends StreamerSubscription> subscriptions) {
        if (subscriptions.size() == 0) {
            return List.of(new EmbedBuilder().setTitle("No subscriptions present."));
        }

        final int subscriptionSize = subscriptions.size();
        List<String> streamerNamesEmbedFields = new ArrayList<>();
        List<String> pingChannelsEmbedFields = new ArrayList<>();
        List<String> pingRolesEmbedFields = new ArrayList<>();

        // max twitch username length is 25; so separate subscription sub lists based off that
        final int singleMaxLineLengthEstimate = 25;
        final int maxFieldCharLength = 1024;
        final int maxLinesPerField = maxFieldCharLength / singleMaxLineLengthEstimate;
        final int numEmbedsNeeded = Math.ceilDiv(subscriptionSize, maxLinesPerField);
        final String delimiter = "\n";
        Logger.trace("Running embedTwitchSubs with numEmbedsNeeded={} for subscription_size={}", numEmbedsNeeded, subscriptionSize);
        int subListsSizeSum = 0;
        for (int embedNum = 1; embedNum <= numEmbedsNeeded; embedNum++) {
            int subListStart = (embedNum - 1) * (maxLinesPerField + 1);
            int subListEnd = embedNum == numEmbedsNeeded ? subscriptionSize : embedNum * maxLinesPerField;
            List<? extends StreamerSubscription> subList = subscriptions.subList(subListStart, subListEnd);
            subListsSizeSum += subList.size();
            streamerNamesEmbedFields.add(renderStreamerNames(subList, delimiter));
            pingChannelsEmbedFields.add(renderPingChannels(subList, delimiter));
            pingRolesEmbedFields.add(renderPingRoles(subList, delimiter));
        }

        if (subListsSizeSum != subscriptionSize) {
            Logger.error("Discrepancy between number of subscriptions on embed ({}) vs database ({}).", subListsSizeSum, subscriptionSize);
        }

        String subType = subscriptions.get(0).getClass().getSimpleName(); // will match file name without .java at the end
        Color embedColor = null;
        switch (subType) {
            case "TwitchSub": 
                embedColor = Constants.twitch_purple;
                subType = "Twitch";
                break;
            default: Logger.error("Received unknown StreamerSubscription type: {}", subType);
        }

        List<EmbedBuilder> embeds = new ArrayList<EmbedBuilder>();
        for (int embed = 0; embed < numEmbedsNeeded; embed++) {
            embeds.add(new EmbedBuilder()
                .setTitle(subType + " Subscriptions")
                .setColor(embedColor)
                .addInlineField("Streamer", streamerNamesEmbedFields.get(embed))
                .addInlineField("Ping Channel", pingChannelsEmbedFields.get(embed))
                .addInlineField("Ping Role", pingRolesEmbedFields.get(embed)));
        }

        return embeds;
    }

    private static String renderStreamerName(StreamerSubscription subscription) {
        final String subType = subscription.getClass().getSimpleName(); // will be TwitchSub for example
        try {
            switch (subType) {
                case "TwitchSub": return TwitchAPI.getUserById(subscription.broadcaster_id).getDisplayName();
                case "YoutubeSub": //TODO
                default:
                    Logger.error("Received unknown StreamerSubscription type: {}", subType);
                    return subscription.broadcaster_id;
            }
        } catch (TwitchApiException | InvalidArgumentException e) {
            Logger.debug(e, "Failed to pull streamer name of broadcaster id: {}", subscription.broadcaster_id);
            return subscription.broadcaster_id;
        }
    }
    private static String renderStreamerNames(List<? extends StreamerSubscription> subscriptions, String delimiter) {
        StringBuilder str = new StringBuilder(subscriptions.size() * 12);
        for (StreamerSubscription sub : subscriptions) {
            str.append(renderStreamerName(sub));
            str.append(delimiter);
        }
        return str.toString();
    }

    private static String renderPingChannel(StreamerSubscription subscription) {
        return "<#" + subscription.pingchannel_id + ">";
    }
    private static String renderPingChannels(List<? extends StreamerSubscription> subscriptions, String delimiter) {
        StringBuilder str = new StringBuilder(subscriptions.size() * 22);
        for (StreamerSubscription sub : subscriptions) {
            str.append(renderPingChannel(sub));
            str.append(delimiter);
        }
        return str.toString();
    }

    private static String renderPingRole(StreamerSubscription subscription) {
        return "<@&" + subscription.pingrole_id + ">";
    }
    private static String renderPingRoles(List<? extends StreamerSubscription> subscriptions, String delimiter) {
        StringBuilder str = new StringBuilder(subscriptions.size() * 24);
        for (StreamerSubscription sub : subscriptions) {
            str.append(renderPingRole(sub));
            str.append(delimiter);
        }
        return str.toString();
    }
}
