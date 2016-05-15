package com.nicholasnassar.nplay;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import spark.ModelAndView;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class WebHandler {
    public WebHandler(nPlay play) {
        port(8999);

        staticFileLocation("/web");

        Map<String, String> emptyMap = new HashMap<>();

        get("/", (req, res) -> new ModelAndView(emptyMap, "index"), new JadeTemplateEngine());

        get("/play", (req, res) -> {
            play.setPlaying(true);

            return "";
        });

        get("/pause", (req, res) -> {
            play.setPlaying(false);

            return "";
        });

        post("/play-url", (req, res) -> {
            String url = req.queryParams("url");

            if (url == null) {
                return "";
            }

            if (url.isEmpty()) {
                play.setUrl("");

                return "";
            }

            UrlValidator validator = new UrlValidator(new String[]{"http", "https"});

            if (validator.isValid(url)) {
                new Thread(() -> {
                    try {
                        HttpURLConnection.setFollowRedirects(false);

                        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

                        con.setRequestMethod("HEAD");

                        if (con.getContentType().startsWith("video")) {
                            play.setUrl(url);

                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Document document = Jsoup.connect(url).get();

                        Elements elements = document.body().getElementsByTag("video");

                        Element first = elements.first();

                        if (first != null) {
                            String source = null;

                            if (first.hasAttr("source")) {
                                source = first.attr("source");

                                play.setUrl(getSource(validator.isValid(source), url, source));
                            } else {
                                Element sourceTag = first.getElementsByTag("source").first();

                                if (sourceTag != null) {
                                    source = sourceTag.attr("src");

                                    play.setUrl(getSource(validator.isValid(source), url, source));
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                ).start();
            }

            return "";
        });

        get("/play-status", (req, res) -> {
            res.type("text/event-stream;charset=UTF-8");
            res.header("Cache-Control", "no-cache");

            return "retry: 500\ndata: {\"url\": \"" + play.getUrl() + "\", \"seconds\": " + play.getCurrentTime()
                    + ", \"playing\":" + play.isPlaying() + "}\n\n";
        });

        get("/seek", (req, res) -> {
            String time = req.queryParams("currentTime");

            if (time == null) {
                return "";
            }

            double currentTime = Double.parseDouble(time);

            play.setCurrentTime(currentTime);

            return "";
        });
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

    public void stop() {
        Spark.stop();
    }
}
