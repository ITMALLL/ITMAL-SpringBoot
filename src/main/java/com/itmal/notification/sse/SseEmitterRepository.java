package com.itmal.notification.sse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseEmitterRepository {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(Long userId, SseEmitter emitter){
        emitters.put(userId, emitter);
    }

    public SseEmitter get(Long userId){
        return emitters.get(userId);
    }

    public void remove(Long userId){
        emitters.remove(userId);
    }
}
