package com.nicholasnassar.nplay;

import org.jsoup.nodes.Document;

@FunctionalInterface
public interface LinkHandler {
    String getVideo(Document document);
}
