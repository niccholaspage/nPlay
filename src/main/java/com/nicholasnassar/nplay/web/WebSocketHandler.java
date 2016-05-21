package com.nicholasnassar.nplay.web;

import com.nicholasnassar.nplay.Channel;
import com.nicholasnassar.nplay.nPlay;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebSocket
public class WebSocketHandler {
    private static Map<Session, String> sessionChannelMap = new HashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        sessionChannelMap.put(user, null);
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        sessionChannelMap.remove(user);
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        if (message.startsWith("channel,")) {
            String channelName = message.substring(message.indexOf(",") + 1);

            Channel channel = nPlay.play.getChannel(channelName);

            if (channel != null) {
                sessionChannelMap.replace(user, channelName);
            }

            return;
        }

        Channel channel = nPlay.play.accessChannel(sessionChannelMap.get(user));

        if (channel == null) {
            return;
        }

        if (message.equals("play")) {
            channel.setPlaying(true);
        } else if (message.equals("pause")) {
            channel.setPlaying(false);
        } else if (message.startsWith("seek,")) {
            double currentTime = Double.parseDouble(message.substring(message.indexOf(",") + 1));

            channel.setCurrentTime(currentTime);
        } else if (message.startsWith("play-url,")) {
            channel.fetchUrl(message.substring(message.indexOf(",") + 1));
        }
    }

    public static void sendStatusUpdate() {
        sessionChannelMap.entrySet().stream().filter(entry -> entry.getValue() != null && entry.getKey().isOpen()).forEach(entry -> {
            try {
                Channel channel = nPlay.play.accessChannel(entry.getValue());

                if (channel != null) {
                    entry.getKey().getRemote().sendString("{\"url\": \"" + channel.getUrl() + "\", \"seconds\": "
                            + channel.getCurrentTime() + ", \"playing\":" + channel.isPlaying() + "}");
                } else {
                    entry.getKey().getRemote().sendString("{}");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
