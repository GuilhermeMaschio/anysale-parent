package com.anysale.notification.adapters.in.messaging;

import com.anysale.contracts.event.LeadUpdatedEvent;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.*;
import java.util.concurrent.*;

public class LeadUpdatedListener {

    private static final Map<UUID, List<String>> store =
            new ConcurrentHashMap<>();

    @KafkaListener(topics = "lead.updated", groupId = "notification-service")
    public void on(LeadUpdatedEvent evt){
        String change = evt.getReason();
        store.computeIfAbsent(evt.getId(), k -> new CopyOnWriteArrayList<>())
                .add("change=" + change + ", stage=" + evt.getStage());
    }

    public static List<String> byLead(UUID id){
        return store.getOrDefault(id, List.of());
    }
}
