package com.nicholasnassar.nplay;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;

public class Channel {
    private final JBrowserDriver browser;

    private String url;

    private double currentTime;

    private boolean playing;

    private double secondsBeforeRemoval;

    public Channel() {
        browser = new JBrowserDriver(Settings.builder().timezone(Timezone.AMERICA_NEWYORK).build());

        secondsBeforeRemoval = 5 * 60;
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
}
