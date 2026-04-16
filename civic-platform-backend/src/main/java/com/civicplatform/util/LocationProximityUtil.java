package com.civicplatform.util;

import com.civicplatform.entity.Event;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Heuristic "nearby" scoring between a citizen {@code address} and an event's holding place,
 * using {@link Event#getLocation()}, {@link Event#getTitle()}, and {@link Event#getDescription()}
 * (text-only; no geocoding).
 */
public final class LocationProximityUtil {

    private static final int MAX_DESCRIPTION_CHARS = 600;

    /** Noise words in addresses — not used as place identifiers. */
    private static final Set<String> STOPWORDS = Set.of(
            "de", "la", "le", "les", "du", "des", "et", "en", "au", "aux", "d", "l", "un", "une",
            "rue", "avenue", "av", "bd", "boulevard", "place", "impasse", "chemin", "route", "lot",
            "numero", "num", "n", "cp", "code", "postal", "tn", "the", "of", "and", "near", "st",
            "street", "road", "apt", "apartment", "bat", "batiment", "bloc", "imm", "immeuble",
            "zone", "industriel", "industrielle", "commune", "gouvernorat");

    /**
     * Greater Tunis / common locality substrings (accent-folded, spaces removed for matching).
     * Both address and venue must mention the same phrase for a small "same area" bonus.
     */
    private static final String[] REGION_PHRASES = {
            "tunis", "ariana", "manouba", "carthage", "marsa", "medina", "ettadhamon", "ettadhamen",
            "soukra", "goulette", "bardo", "kram", "hammam", "lif", "arous", "benarous", "sidibousaid",
            "sidihassine", "lamarsa", "lagoulette", "lac", "jardins"
    };

    private LocationProximityUtil() {
    }

    /**
     * @param maxPoints cap for this feature (e.g. 15.0 in the rate engine)
     */
    public static double scoreAddressToEventVenue(String citizenAddress, Event event, double maxPoints) {
        if (citizenAddress == null || citizenAddress.isBlank() || event == null) {
            return 0.0;
        }
        String venueCorpus = buildVenueCorpus(event);
        if (venueCorpus.isBlank()) {
            return 0.0;
        }
        String normAddr = normalize(citizenAddress);
        String normVenue = normalize(venueCorpus);
        if (normAddr.isEmpty() || normVenue.isEmpty()) {
            return 0.0;
        }

        Set<String> addrTokens = significantTokens(normAddr);
        Set<String> venueTokens = significantTokens(normVenue);
        if (addrTokens.isEmpty() || venueTokens.isEmpty()) {
            return 0.0;
        }

        double raw = 0.0;
        Set<String> consumed = new HashSet<>();

        // 1) Strong: identical significant token on address and venue (neighborhood, district)
        for (String t : addrTokens) {
            if (t.length() < 3) {
                continue;
            }
            if (venueTokens.contains(t) && consumed.add("eq:" + t)) {
                raw += 5.0;
            }
        }

        // 2) Medium: address token appears inside full venue text (e.g. district name in title)
        for (String t : addrTokens) {
            if (t.length() < 4) {
                continue;
            }
            if (normVenue.contains(t) && consumed.add("inVenue:" + t)) {
                raw += 4.0;
            }
        }

        // 3) Medium: venue token appears inside full address
        for (String t : venueTokens) {
            if (t.length() < 4) {
                continue;
            }
            if (normAddr.contains(t) && consumed.add("inAddr:" + t)) {
                raw += 4.0;
            }
        }

        raw += sameRegionBonus(normAddr, normVenue, consumed);

        return Math.min(maxPoints, raw);
    }

    public static String buildVenueCorpus(Event event) {
        if (event == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (event.getLocation() != null) {
            sb.append(event.getLocation()).append(' ');
        }
        if (event.getTitle() != null) {
            sb.append(event.getTitle()).append(' ');
        }
        if (event.getDescription() != null) {
            String d = event.getDescription();
            if (d.length() > MAX_DESCRIPTION_CHARS) {
                d = d.substring(0, MAX_DESCRIPTION_CHARS);
            }
            sb.append(d);
        }
        return sb.toString().trim();
    }

    private static double sameRegionBonus(String normAddr, String normVenue, Set<String> consumed) {
        String compactAddr = normAddr.replace(" ", "");
        String compactVenue = normVenue.replace(" ", "");
        double bonus = 0.0;
        for (String phrase : REGION_PHRASES) {
            if (phrase.length() < 4) {
                continue;
            }
            if (compactAddr.contains(phrase) && compactVenue.contains(phrase) && consumed.add("region:" + phrase)) {
                bonus += 3.0;
            }
        }
        return Math.min(6.0, bonus);
    }

    private static String normalize(String s) {
        String folded = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String lower = folded.toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^a-z0-9\\s]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static Set<String> significantTokens(String normalizedLowercase) {
        String[] parts = normalizedLowercase.split("\\s+");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(t -> t.length() >= 3)
                .filter(t -> !STOPWORDS.contains(t))
                .collect(Collectors.toCollection(HashSet::new));
    }
}
