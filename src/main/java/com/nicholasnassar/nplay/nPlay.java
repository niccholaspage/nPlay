package com.nicholasnassar.nplay;

import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class nPlay {
    private final WebHandler webHandler;

    private String url;

    private double currentTime;

    private boolean playing;

    private nPlay() {
        this.webHandler = new WebHandler(this);

        log("Welcome to nPlay!");

        setUrl("");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(() -> {
            if (isPlaying()) {
                setCurrentTime(getCurrentTime() + 1);
            }
        }, 0, 1, TimeUnit.MILLISECONDS);

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

    public void setUrl(String url) {
        this.url = url;

        currentTime = 0;

        playing = !url.isEmpty();
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
