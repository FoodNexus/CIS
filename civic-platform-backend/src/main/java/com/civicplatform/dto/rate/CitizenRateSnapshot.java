package com.civicplatform.dto.rate;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Multi-feature rate for a citizen in the context of a specific event and donor.
 * {@code compositeRate} is 0–100 for thresholds and UI.
 */
@Value
@Builder
public class CitizenRateSnapshot {
    double rawTotal;
    double compositeRate;
    Map<String, Double> features;
}
