package com.nicholasnassar.nplay;

import spark.ModelAndView;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

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

            play.setUrl(url);

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

    public void stop() {
        Spark.stop();
    }
}
