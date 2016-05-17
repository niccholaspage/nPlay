package com.nicholasnassar.nplay;

import spark.ModelAndView;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class WebHandler {
    public WebHandler(nPlay play) {
        port(8999);

        staticFileLocation("/web");

        Map<String, String> emptyMap = new HashMap<>();

        get("/", (req, res) -> new ModelAndView(emptyMap, "index"), new JadeTemplateEngine());
        get("/channel/create", (req, res) -> {
            res.redirect("/channel/" + play.createNewChannel());

            return "";
        });
        get("/channel/:id", (req, res) -> {
            String id = req.params(":id");

            if (!play.hasChannel(id)) {
                res.redirect("/");

                return "";
            }

            try {
                Cookie cookie = new Cookie("channel", id);
                cookie.setPath("/");
                cookie.setMaxAge(-1);
                cookie.setSecure(false);
                cookie.setHttpOnly(false);
                res.raw().addCookie(cookie);
            } catch (Exception e) {
                e.printStackTrace();
            }

            res.redirect("/player");

            return "";
        });
        get("/player", (req, res) -> {
            if (!play.hasChannel(req.cookie("channel"))) {
                res.redirect("/");

                return null;
            }

            return new ModelAndView(emptyMap, "player");
        }, new JadeTemplateEngine());
        get("/about", (req, res) -> new ModelAndView(emptyMap, "about"), new JadeTemplateEngine());
        get("/contact", (req, res) -> new ModelAndView(emptyMap, "contact"), new JadeTemplateEngine());

        get("/play", (req, res) -> {
            Channel channel = accessChannel(play, req.cookie("channel"));

            if (channel != null) {
                channel.setPlaying(true);
            }

            return "";
        });

        get("/pause", (req, res) -> {
            Channel channel = accessChannel(play, req.cookie("channel"));

            if (channel != null) {
                channel.setPlaying(false);
            }

            return "";
        });

        post("/play-url", (req, res) -> {
            String url = req.queryParams("url");

            Channel channel = accessChannel(play, req.cookie("channel"));

            if (channel != null) {
                channel.fetchUrl(url);
            }

            return "";
        });

        get("/play-status", (req, res) -> {
            res.type("text/event-stream;charset=UTF-8");
            res.header("Cache-Control", "no-cache");

            Channel channel = accessChannel(play, req.cookie("channel"));

            if (channel != null) {
                return "retry: 500\ndata: {\"url\": \"" + channel.getUrl() + "\", \"seconds\": " + channel.getCurrentTime()
                        + ", \"playing\":" + channel.isPlaying() + "}\n\n";
            } else {
                return "retry:500\ndata: {}\n\n";
            }
        });

        get("/seek", (req, res) -> {
            String time = req.queryParams("currentTime");

            if (time == null) {
                return "";
            }

            Channel channel = play.getChannel(req.cookie("channel"));

            if (channel != null) {
                double currentTime = Double.parseDouble(time);

                channel.setCurrentTime(currentTime);
            }

            return "";
        });
    }

    public Channel accessChannel(nPlay play, String id) {
        Channel channel = play.getChannel(id);

        if (channel != null) {
            channel.resetSecondsLeft();

            return channel;
        }

        return null;
    }

    public void stop() {
        Spark.stop();
    }
}
