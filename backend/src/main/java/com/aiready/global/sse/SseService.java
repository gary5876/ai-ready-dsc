package com.aiready.global.sse;

import com.aiready.job.dto.JobStatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long jobId) {
        SseEmitter emitter = new SseEmitter(180_000L);
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));
        return emitter;
    }

    public void push(Long jobId, JobStatusEvent event) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event().data(event));
            if ("DONE".equals(event.status()) || "FAILED".equals(event.status())) {
                emitter.complete();
            }
        } catch (IOException e) {
            emitters.remove(jobId);
        }
    }
}
