package com.anysale.notification.adapters.in.rest;

import com.anysale.notification.adapters.in.messaging.LeadUpdatedListener;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/v1/notifications")
public class NotificationController {
    @GetMapping("/{leadId}")
    public List<String> list(@PathVariable UUID leadId){
        return LeadUpdatedListener.byLead(leadId);
    }
}
