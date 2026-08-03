package com.scraper.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CrawlProgressBroadcaster {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long configId) {
        SseEmitter emitter = new SseEmitter(600_000L);
        emitters.put(configId, emitter);
        emitter.onCompletion(() -> emitters.remove(configId));
        emitter.onTimeout(() -> emitters.remove(configId));
        emitter.onError(e -> emitters.remove(configId));
        return emitter;
    }

    public void broadcast(Long configId, String event, Map<String, Object> data) {
        SseEmitter emitter = emitters.get(configId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name(event)
                    .data(data));
        } catch (IOException e) {
            log.warn("Failed to send SSE event for config {}: {}", configId, event);
            emitters.remove(configId);
        }
    }

    public void sendStart(Long configId, String configName, int totalSites) {
        broadcast(configId, "crawl-start", Map.of(
                "configId", configId,
                "configName", configName,
                "totalSites", totalSites
        ));
    }

    public void sendSiteStart(Long configId, String siteName, int index, int total) {
        broadcast(configId, "site-start", Map.of(
                "siteName", siteName,
                "index", index,
                "total", total
        ));
    }

    public void sendSiteComplete(Long configId, String siteName, int jobCount, boolean success, String error) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("siteName", siteName);
        data.put("jobCount", jobCount);
        data.put("success", success);
        if (error != null) data.put("error", error);
        broadcast(configId, "site-complete", data);
    }

    public void sendCrawlComplete(Long configId, int totalSites, int successSites, int totalJobs, int newJobs, int dupJobs) {
        broadcast(configId, "crawl-complete", Map.of(
                "totalSites", totalSites,
                "successSites", successSites,
                "totalJobs", totalJobs,
                "newJobs", newJobs,
                "dupJobs", dupJobs
        ));
        emitters.remove(configId);
    }
}
