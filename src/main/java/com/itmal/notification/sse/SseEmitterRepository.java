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

    public void remove(Long userId, SseEmitter emitter){
        // 현재 저장된 emitter와 같은 인스턴스일 때만 제거 (재연결 시 새 emitter 보호)
        emitters.remove(userId, emitter);
    }
}
