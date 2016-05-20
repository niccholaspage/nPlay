package com.nicholasnassar.nplay.web;

import com.nicholasnassar.nplay.nPlay;
import spark.ModelAndView;
import spark.Spark;
import spark.template.jade.JadeTemplateEngine;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class WebHandler {
    private final nPlay play;

    public WebHandler(nPlay play) {
        this.play = play;

        port(8999);

        staticFileLocation("/web");

        Map<String, String> emptyMap = new HashMap<>();

        webSocket("/play-status", WebSocketHandler.class);

        get("/", (req, res) -> new ModelAndView(emptyMap, "index"), new JadeTemplateEngine());
        get("/channel/create", (req, res) -> {
            String channelId = play.createNewChannel();

            if (channelId == null) {
                res.redirect("/");

                return "";
            }

            res.redirect("/channel/" + channelId);

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
    }

    public void stop() {
        Spark.stop();
    }
}
