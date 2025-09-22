package pingping.Discord.Helpers;

import java.util.ArrayList;
import java.util.List;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.tinylog.Logger;

import pingping.Database.OrmObjects.TwitchSub;
import pingping.Discord.Constants;
import pingping.Exceptions.InvalidArgumentException;
import pingping.Exceptions.TwitchApiException;
import pingping.Twitch.TwitchAPI;

public class SubscriptionsEmbed {
    /**
     * returns a list of embeds needed to show all subscriptions for a server
     * @param subscriptions
     */
    public static List<EmbedBuilder> embedTwitchSubs(List<TwitchSub> subscriptions) {
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
            List<TwitchSub> subList = subscriptions.subList(subListStart, subListEnd);
            subListsSizeSum += subList.size();
            streamerNamesEmbedFields.add(renderStreamerNames(subList, delimiter));
            pingChannelsEmbedFields.add(renderPingChannels(subList, delimiter));
            pingRolesEmbedFields.add(renderPingRoles(subList, delimiter));
        }

        if (subListsSizeSum != subscriptionSize) {
            Logger.error("Discrepancy between number of subscriptions on embed ({}) vs database ({}).", subListsSizeSum, subscriptionSize);
        }

        List<EmbedBuilder> embeds = new ArrayList<EmbedBuilder>();
        for (int embed = 0; embed < numEmbedsNeeded; embed++) {
            embeds.add(new EmbedBuilder()
                .setTitle("Twitch Subscriptions")
                .setColor(Constants.twitch_purple)
                .addInlineField("Streamer", streamerNamesEmbedFields.get(embed))
                .addInlineField("Ping Channel", pingChannelsEmbedFields.get(embed))
                .addInlineField("Ping Role", pingRolesEmbedFields.get(embed)));
        }

        return embeds;
    }

    private static String renderStreamerName(TwitchSub subscription) {
        try {
            return TwitchAPI.getUserById(subscription.broadcaster_id).getDisplayName();
        } catch (TwitchApiException | InvalidArgumentException e) {
            Logger.debug(e, "Failed to pull streamer name of broadcaster id: {}", subscription.broadcaster_id);
            return Long.toString(subscription.broadcaster_id);
        }
    }
    private static String renderStreamerNames(List<TwitchSub> subscriptions, String delimiter) {
        StringBuilder str = new StringBuilder(subscriptions.size() * 12);
        for (TwitchSub sub : subscriptions) {
            str.append(renderStreamerName(sub));
            str.append(delimiter);
        }
        return str.toString();
    }

    private static String renderPingChannel(TwitchSub subscription) {
        return "<#" + subscription.pingchannel_id + ">";
    }
    private static String renderPingChannels(List<TwitchSub> subscriptions, String delimiter) {
        StringBuilder str = new StringBuilder(subscriptions.size() * 22);
        for (TwitchSub sub : subscriptions) {
            str.append(renderPingChannel(sub));
            str.append(delimiter);
        }
        return str.toString();
    }

    private static String renderPingRole(TwitchSub subscription) {
        return "<@&" + subscription.pingrole_id + ">";
    }
    private static String renderPingRoles(List<TwitchSub> subscriptions, String delimiter) {
        StringBuilder str = new StringBuilder(subscriptions.size() * 24);
        for (TwitchSub sub : subscriptions) {
            str.append(renderPingRole(sub));
            str.append(delimiter);
        }
        return str.toString();
    }
}
