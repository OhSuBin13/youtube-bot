package com.example.youtubebot.oauth;

public record YouTubeChannelIdentity(String channelId, String channelName) {

    public static final int MAX_CHANNEL_ID_LENGTH = 64;
    public static final int MAX_CHANNEL_NAME_LENGTH = 255;

    public YouTubeChannelIdentity {
        if (channelId == null
                || channelId.isBlank()
                || channelId.length() > MAX_CHANNEL_ID_LENGTH) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.INVALID_CHANNEL,
                    "YouTube channel ID is invalid");
        }
        if (channelName == null
                || channelName.isBlank()
                || channelName.length() > MAX_CHANNEL_NAME_LENGTH) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.INVALID_CHANNEL,
                    "YouTube channel name is invalid");
        }
    }
}
