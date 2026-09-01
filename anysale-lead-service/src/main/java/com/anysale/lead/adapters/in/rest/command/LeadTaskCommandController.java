package com.anysale.lead.adapters.in.rest.command;
import com.anysale.lead.adapters.in.rest.dto.*; import com.anysale.lead.aplication.LeadTaskService; import com.anysale.lead.domain.model.LeadTask; import jakarta.validation.Valid; import java.net.URI; import java.util.UUID; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/v1") public class LeadTaskCommandController {
 private final LeadTaskService service; public LeadTaskCommandController(LeadTaskService service){this.service=service;}
 @PostMapping("/leads/{leadId}/tasks") public ResponseEntity<LeadTaskResponse> create(@PathVariable UUID leadId,@Valid @RequestBody LeadTaskCreateRequest body){LeadTask t=service.create(leadId,body);return ResponseEntity.created(URI.create("/v1/tasks/"+t.getId())).body(dto(t));}
 @PostMapping("/tasks/{id}/claim") public LeadTaskResponse claim(@PathVariable UUID id){return dto(service.claim(id));}
 @PostMapping("/tasks/{id}/release") public LeadTaskResponse release(@PathVariable UUID id){return dto(service.release(id));}
 @PostMapping("/tasks/{id}/complete") public LeadTaskResponse complete(@PathVariable UUID id,@Valid @RequestBody LeadTaskCompleteRequest body){return dto(service.complete(id,body));}
 @PostMapping("/tasks/{id}/snooze") public LeadTaskResponse snooze(@PathVariable UUID id,@Valid @RequestBody LeadTaskSnoozeRequest body){return dto(service.snooze(id,body));}
 public static LeadTaskResponse dto(LeadTask t){return new LeadTaskResponse(t.getId(),t.getLead().getId(),t.getLead().getName(),t.getTitle(),t.getTaskType(),t.getPriority(),t.getStatus(),t.getDueAt(),t.getAssignedTo(),t.getReservationExpiresAt(),t.getCompletedAt(),t.getOutcome(),t.getNote(),t.getCreatedAt());}
}
