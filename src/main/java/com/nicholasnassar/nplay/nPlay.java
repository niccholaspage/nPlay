package com.nicholasnassar.nplay;

import com.nicholasnassar.nplay.web.WebHandler;
import com.nicholasnassar.nplay.web.WebSocketHandler;
import org.json.JSONObject;
import org.jsoup.nodes.Element;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class nPlay {
    private final Map<String, LinkHandler> handlers;

    private final Map<String, Channel> channels;

    private final Random random;

    private final WebHandler webHandler;

    private final static String CHANNEL_ID_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final static int CHANNEL_ID_LENGTH = 7;

    private final static int CHANNEL_LIMIT = 5;

    public static nPlay play;

    private nPlay() {
        play = this;

        channels = new HashMap<>();

        channels.put("main", new Channel(this, "main", true));

        random = new Random();

        handlers = new HashMap<>();

        handlers.put("youtube.com", document -> {
            String videoUrl = "";

            for (Element element : document.getElementsByTag("script")) {
                String html = element.html();

                if (html.startsWith("var ytplayer = ytplayer")) {
                    int argsStart = html.indexOf("\"args\":{") + 7;

                    String args = html.substring(argsStart, html.indexOf("},", argsStart) + 1);

                    JSONObject json = new JSONObject(args);

                    try {
                        String urlEncoded = URLDecoder.decode(json.getString("url_encoded_fmt_stream_map"), "UTF-8");

                        System.out.println(urlEncoded);

                        int urlStart = urlEncoded.indexOf("url=") + 4;

                        videoUrl = urlEncoded.substring(urlStart, urlEncoded.indexOf(";", urlStart));

                        videoUrl = videoUrl.replace(",type", "&type");

                        System.out.println(videoUrl);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }

            return videoUrl;
        });

        this.webHandler = new WebHandler(this);

        log("Welcome to nPlay!");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> channels.values().stream().filter(Channel::isPlaying).forEach(channel -> channel.setCurrentTime(channel.getCurrentTime() + 0.001)), 0, 1, TimeUnit.MILLISECONDS);

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                WebSocketHandler.sendStatusUpdate();
            }
        }, 0, 500);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                channels.entrySet().removeIf(entry -> {
                    Channel channel = entry.getValue();

                    if (channel.getSecondsBeforeRemoval() != -1) {
                        channel.setSecondsBeforeRemoval(channel.getSecondsBeforeRemoval() - 1);

                        if (channel.getSecondsBeforeRemoval() <= 0) {
                            channel.close();

                            return true;
                        }
                    }

                    return false;
                });
            }
        }, 0, 1000);

        Scanner scanner = new Scanner(System.in);

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                log("You need to enter something in!");
            }

            String[] split = line.split(" ");

            String command = split[0];

            if (command.equalsIgnoreCase("channels")) {
                log("Channels:");

                channels.entrySet().forEach(entry -> log(entry.getKey() + " - " + entry.getValue().getSecondsBeforeRemoval() + " seconds left"));
            } else if (command.equalsIgnoreCase("quit")) {
                System.exit(0);
            }
        }
    }

    public boolean hasChannel(String id) {
        return channels.containsKey(id);
    }

    public Channel getChannel(String id) {
        return channels.get(id);
    }

    public String createNewChannel() {
        if (channels.size() >= CHANNEL_LIMIT) {
            return null;
        }

        String id = generateChannelId();

        while (hasChannel(id)) {
            id = generateChannelId();
        }

        Channel channel = new Channel(this, id);

        channels.put(id, channel);

        return id;
    }

    private String generateChannelId() {
        StringBuilder builder = new StringBuilder(CHANNEL_ID_LENGTH);

        for (int i = 0; i < CHANNEL_ID_LENGTH; i++) {
            builder.append(CHANNEL_ID_CHARACTERS.charAt(random.nextInt(CHANNEL_ID_CHARACTERS.length())));
        }

        return builder.toString();
    }

    public Channel accessChannel(String id) {
        Channel channel = getChannel(id);

        if (channel != null) {
            channel.resetSecondsLeft();

            return channel;
        }

        return null;
    }

    private void log(String message) {
        System.out.println(message);
    }

    private void stop() {
        log("Shutting down...");

        channels.values().forEach(Channel::close);

        webHandler.stop();
    }

    public static void main(String[] args) {
        new nPlay();
    }

    public Map<String, LinkHandler> getHandlers() {
        return handlers;
    }
}
