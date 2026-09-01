package com.anysale.lead.adapters.in.rest.query;
import com.anysale.lead.adapters.in.rest.command.LeadTaskCommandController; import com.anysale.lead.adapters.in.rest.dto.LeadTaskResponse; import com.anysale.lead.aplication.LeadTaskService; import java.util.*; import org.springframework.data.domain.Page; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/v1") public class LeadTaskQueryController { private final LeadTaskService service; public LeadTaskQueryController(LeadTaskService service){this.service=service;}
 @GetMapping("/tasks") public Page<LeadTaskResponse> queue(@RequestParam(defaultValue="available") String view,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.queue(view,page,Math.min(size,100)).map(LeadTaskCommandController::dto);}
 @GetMapping("/leads/{leadId}/tasks") public List<LeadTaskResponse> byLead(@PathVariable UUID leadId){return service.byLead(leadId).stream().map(LeadTaskCommandController::dto).toList();}
}
