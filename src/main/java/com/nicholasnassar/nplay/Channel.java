package com.nicholasnassar.nplay;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.nicholasnassar.nplay.web.WebSocketHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class Channel {
    private final nPlay play;

    private final JBrowserDriver browser;

    private final String name;

    private String url;

    private double currentTime;

    private boolean playing;

    private double secondsBeforeRemoval;

    private static final int SECONDS_BEFORE_REMOVAL = 1 * 60;

    public Channel(nPlay play, JBrowserDriver browser, String name) {
        this(play, browser, name, false);
    }

    public Channel(nPlay play, JBrowserDriver browser, String name, boolean indefinite) {
        this.play = play;

        this.browser = browser;

        this.name = name;

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
        browser.quit();
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
            new Thread(() -> {
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

                    browser.get(url);

                    try {
                        URL tempUrl = new URL(url);

                        String baseUrl = tempUrl.getProtocol() + "://" + tempUrl.getAuthority() + "/";

                        for (Map.Entry<String, LinkHandler> handler : play.getHandlers().entrySet()) {
                            if (baseUrl.contains(handler.getKey())) {
                                sendStatus("");

                                setUrl(handler.getValue().getVideo(browser));

                                return;
                            }
                        }
                    } catch (Exception e) {
                        sendStatus("e:Problem with link handlers. Oops.");
                    }

                    List<WebElement> elements = browser.findElements(By.tagName("video"));

                    if (!elements.isEmpty()) {
                        WebElement first = elements.get(0);

                        if (first != null) {
                            String source = null;

                            if (first.getAttribute("src") != null) {
                                source = first.getAttribute("src");

                                sendStatus("");

                                setUrl(getSource(validator.isValid(source), url, source));
                            } else {
                                WebElement sourceTag = first.findElements(By.tagName("source")).get(0);

                                if (sourceTag != null) {
                                    source = sourceTag.getAttribute("src");

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
