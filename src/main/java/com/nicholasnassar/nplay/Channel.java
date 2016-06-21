package com.nicholasnassar.nplay;

import com.nicholasnassar.nplay.web.WebSocketHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Channel {
    private final nPlay play;

    private final String name;

    private final ExecutorService executor;

    private String url;

    private double currentTime;

    private boolean playing;

    private double secondsBeforeRemoval;

    private static final int SECONDS_BEFORE_REMOVAL = 1 * 60;

    public Channel(nPlay play, String name) {
        this(play, name, false);
    }

    public Channel(nPlay play, String name, boolean indefinite) {
        this.play = play;

        this.name = name;

        executor = Executors.newSingleThreadExecutor();

        if (indefinite) {
            secondsBeforeRemoval = -1;
        }

        resetSecondsLeft();

        setUrl("");
    }

    public void resetSecondsLeft() {
        if (secondsBeforeRemoval != -1) {
            secondsBeforeRemoval = SECONDS_BEFORE_REMOVAL;
        }
    }

    public void close() {
        try {
            executor.shutdown();

            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println("Interrupted tasks!");
        } finally {
            if (!executor.isTerminated()) {
                System.out.println("Still not terminated");
            }

            executor.shutdownNow();
        }
    }

    public double getSecondsBeforeRemoval() {
        return secondsBeforeRemoval;
    }

    public void setSecondsBeforeRemoval(double secondsBeforeRemoval) {
        this.secondsBeforeRemoval = secondsBeforeRemoval;
    }

    public String getUrl() {
        return url;
    }

    private void setUrl(String url) {
        this.url = url;

        currentTime = 0;

        playing = !url.isEmpty();
    }

    public void sendStatus(String status) {
        WebSocketHandler.sendStatusMessage(name, status);
    }

    public void fetchUrl(String url) {
        if (url == null) {
            return;
        }

        if (url.isEmpty()) {
            sendStatus("");

            setUrl("");

            return;
        }

        sendStatus("i:Fetching video...");

        //Empty video for now.
        setUrl("");

        UrlValidator validator = new UrlValidator(new String[]{"http", "https"});

        if (validator.isValid(url)) {
            executor.submit(() -> {
                try {
                    HttpURLConnection.setFollowRedirects(false);

                    HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

                    con.setRequestMethod("HEAD");

                    if (con.getContentType().contains("video/")) {
                        sendStatus("");

                        setUrl(url);

                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    sendStatus("i:Fetching video from URL...");

                    Document document = Jsoup.connect(url).get();

                    try {
                        URL tempUrl = new URL(url);

                        String baseUrl = tempUrl.getProtocol() + "://" + tempUrl.getAuthority() + "/";

                        for (Map.Entry<String, LinkHandler> handler : play.getHandlers().entrySet()) {
                            if (baseUrl.contains(handler.getKey())) {
                                sendStatus("");

                                setUrl(handler.getValue().getVideo(document));

                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendStatus("e:Problem with link handlers. Oops.");
                    }

                    List<Element> elements = document.getElementsByTag("video");

                    if (!elements.isEmpty()) {
                        Element first = elements.get(0);

                        if (first != null) {
                            String source = null;

                            if (first.attr("src") != null) {
                                source = first.attr("src");

                                sendStatus("");

                                setUrl(getSource(validator.isValid(source), url, source));
                            } else {
                                Element sourceTag = first.getElementsByTag("source").get(0);

                                if (sourceTag != null) {
                                    source = sourceTag.attr("src");

                                    sendStatus("");

                                    setUrl(getSource(validator.isValid(source), url, source));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    sendStatus("e:Error fetching video! Sorry :(");
                }
            });
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
