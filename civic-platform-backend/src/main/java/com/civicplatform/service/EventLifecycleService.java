package com.civicplatform.service;

public interface EventLifecycleService {

    void onEventStarted(Long eventId);

    void onEventCompleted(Long eventId);
}
