package com.civicplatform.util;

import com.civicplatform.entity.Event;

/**
 * Shared keyword detection for food / solidarity / community-fridge style events and copy.
 */
public final class FoodCommunityContext {

    private FoodCommunityContext() {
    }

    public static boolean matchesEvent(Event event) {
        if (event == null) {
            return false;
        }
        return matchesCorpus(
                (event.getTitle() != null ? event.getTitle() : "")
                        + " "
                        + (event.getDescription() != null ? event.getDescription() : ""));
    }

    public static boolean matchesCorpus(String corpus) {
        if (corpus == null || corpus.isBlank()) {
            return false;
        }
        String t = corpus.toLowerCase();
        return t.contains("food")
                || t.contains("distribution")
                || t.contains("cooking")
                || t.contains("recipe")
                || t.contains("ramadan")
                || t.contains("iftar")
                || t.contains("solidarity")
                || t.contains("fridge")
                || t.contains("restock")
                || t.contains("leftover")
                || t.contains("food waste")
                || t.contains("community kitchen")
                || t.contains("hunger")
                || (t.contains("sustainability") && (t.contains("food") || t.contains("waste") || t.contains("kitchen")));
    }
}
