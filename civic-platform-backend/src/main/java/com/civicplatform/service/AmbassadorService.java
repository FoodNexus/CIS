package com.civicplatform.service;

import com.civicplatform.dto.response.AmbassadorInfluenceResponse;

import java.util.List;

public interface AmbassadorService {

    /**
     * Returns AMBASSADOR users sorted by deterministic influence score descending.
     */
    List<AmbassadorInfluenceResponse> getRankingByInfluence();
}
