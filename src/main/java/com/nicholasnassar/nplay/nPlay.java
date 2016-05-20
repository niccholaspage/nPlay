package com.nicholasnassar.nplay;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;
import com.nicholasnassar.nplay.web.WebHandler;
import com.nicholasnassar.nplay.web.WebSocketHandler;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class nPlay {
    private final Map<String, LinkHandler> handlers;

    private final Map<String, Channel> channels;

    private final AsyncConstruction<JBrowserDriver> browser;

    private final Random random;

    private final WebHandler webHandler;

    private final static String CHANNEL_ID_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final static int CHANNEL_ID_LENGTH = 7;

    private final static int CHANNEL_LIMIT = 5;

    public static nPlay play;

    private nPlay() {
        play = this;

        channels = new HashMap<>();

        browser = new AsyncConstruction<JBrowserDriver>() {
            @Override
            public JBrowserDriver create() {
                return new JBrowserDriver(Settings.builder().timezone(Timezone.AMERICA_NEWYORK).build());
            }
        };

        channels.put("main", new Channel(this, browser.get(), true));

        random = new Random();

        handlers = new HashMap<>();

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

                channels.keySet().forEach(this::log);
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

        Channel channel = new Channel(this, browser.get());

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


    public void play(Channel channel) {
        channel.setPlaying(true);
    }

    public void pause(Channel channel) {
        channel.setPlaying(false);
    }

    public void playUrl(Channel channel, String url) {
        channel.fetchUrl(url);
    }

    public void seek(Channel channel, String time) {
        double currentTime = Double.parseDouble(time);

        channel.setCurrentTime(currentTime);
    }

    public Channel accessChannel(String id) {
        Channel channel = play.getChannel(id);

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
