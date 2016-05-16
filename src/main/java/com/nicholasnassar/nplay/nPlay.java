package com.nicholasnassar.nplay;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class nPlay {
    private final JBrowserDriver browser;

    private final Map<String, LinkHandler> handlers;

    private final Map<String, Channel> channels;

    private final WebHandler webHandler;

    private String url;

    private double currentTime;

    private boolean playing;

    private nPlay() {
        channels = new HashMap<>();

        handlers = new HashMap<>();

        browser = new JBrowserDriver(Settings.builder().timezone(Timezone.AMERICA_NEWYORK).build());

        this.webHandler = new WebHandler(this);

        log("Welcome to nPlay!");

        setUrl("");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            if (isPlaying()) {
                setCurrentTime(getCurrentTime() + 0.001);
            }
        }, 0, 1, TimeUnit.MILLISECONDS);

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                channels.forEach((id, channel) -> channel.setSecondsBeforeRemoval(channel.getSecondsBeforeRemoval() - 1));

                channels.values().stream().filter(channel -> channel.getSecondsBeforeRemoval() <= 0).forEach(channels::remove);
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

            if (command.equalsIgnoreCase("quit")) {
                System.exit(0);
            }
        }
    }

    private void log(String message) {
        System.out.println(message);
    }

    private void stop() {
        log("Shutting down...");

        webHandler.stop();
    }

    private void setUrl(String url) {
        this.url = url;

        currentTime = 0;

        playing = !url.isEmpty();
    }

    public void fetchUrl(String url) {
        if (url == null) {
            return;
        }

        if (url.isEmpty()) {
            setUrl("");

            return;
        }

        UrlValidator validator = new UrlValidator(new String[]{"http", "https"});

        if (validator.isValid(url)) {
            new Thread(() -> {
                try {
                    HttpURLConnection.setFollowRedirects(false);

                    HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

                    con.setRequestMethod("HEAD");

                    if (con.getContentType().contains("video/")) {
                        setUrl(url);

                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    browser.get(url);

                    try {
                        URL tempUrl = new URL(url);

                        String baseUrl = tempUrl.getProtocol() + "://" + tempUrl.getAuthority() + "/";

                        for (Map.Entry<String, LinkHandler> handler : handlers.entrySet()) {
                            if (baseUrl.contains(handler.getKey())) {
                                setUrl(handler.getValue().getVideo(browser));

                                return;
                            }
                        }
                    } catch (Exception e) {

                    }

                    List<WebElement> elements = browser.findElements(By.tagName("video"));

                    WebElement first = elements.get(0);

                    if (first != null) {
                        String source = null;

                        if (first.getAttribute("src") != null) {
                            source = first.getAttribute("src");

                            setUrl(getSource(validator.isValid(source), url, source));
                        } else {
                            WebElement sourceTag = first.findElements(By.tagName("source")).get(0);

                            if (sourceTag != null) {
                                source = sourceTag.getAttribute("src");

                                setUrl(getSource(validator.isValid(source), url, source));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ).start();
        }
    }

    public String getSource(boolean valid, String url, String source) {
        if (!valid) {
            int baseCount = StringUtils.countMatches(url, "../") + 1;

            while (baseCount > 0) {
                url = url.substring(0, url.lastIndexOf("/"));

                baseCount--;
            }

            url += "/" + source;

            return url;
        } else {
            return source;
        }
    }

    public String getUrl() {
        return url;
    }

    public static void main(String[] args) {
        new nPlay();
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(double currentTime) {
        this.currentTime = currentTime;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        if (url.isEmpty()) {
            return;
        }

        this.playing = playing;
    }
}
