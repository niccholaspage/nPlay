package com.nicholasnassar.nplay;

import com.machinepublishers.jbrowserdriver.JBrowserDriver;

@FunctionalInterface
public interface LinkHandler {
    String getVideo(JBrowserDriver browser);
}
