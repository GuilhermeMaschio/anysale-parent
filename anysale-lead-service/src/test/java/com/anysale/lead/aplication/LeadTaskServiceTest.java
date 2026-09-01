package com.anysale.lead.aplication;

import com.anysale.lead.adapters.in.rest.dto.*;
import com.anysale.lead.adapters.out.persistence.*;
import com.anysale.lead.domain.model.*;
import com.anysale.lead.tenant.*;
import org.junit.jupiter.api.*; import org.junit.jupiter.api.extension.ExtendWith; import org.mockito.*; import org.mockito.junit.jupiter.MockitoExtension; import org.springframework.web.server.ResponseStatusException;
import java.time.*; import java.util.*;
import static org.assertj.core.api.Assertions.*; import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadTaskServiceTest {
 @Mock LeadTaskJpaRepository tasks; @Mock LeadJpaRepository leads; @Mock TenantContext tenants; @Mock UserIdentityContext users; @InjectMocks LeadTaskService service;
 private final UUID leadId=UUID.randomUUID(), taskId=UUID.randomUUID(); private Lead lead;
 @BeforeEach void setup(){lead=new Lead();lead.setId(leadId);lead.setTenantId("tenant-a");lead.setName("Ana");when(tenants.tenantId()).thenReturn("tenant-a");lenient().when(users.userId()).thenReturn("agent-1");}
 @Test void createsTaskForTheLeadTenant(){when(leads.findByIdWithTags(leadId)).thenReturn(Optional.of(lead));when(tasks.save(any())).thenAnswer(i->i.getArgument(0));LeadTask t=service.create(leadId,new LeadTaskCreateRequest("Responder cliente","WHATSAPP_REPLY",null,Instant.now().plusSeconds(60),null));assertThat(t.getTenantId()).isEqualTo("tenant-a");assertThat(t.getStatus()).isEqualTo("OPEN");assertThat(t.getPriority()).isEqualTo("NORMAL");}
 @Test void claimsAvailableTaskAndAssignsLeadOwner(){LeadTask t=open();when(tasks.findScoped(taskId,"tenant-a")).thenReturn(Optional.of(t));when(tasks.countByTenantIdAndAssignedToAndStatus("tenant-a","agent-1","ASSIGNED")).thenReturn(0L);when(tasks.save(any())).thenAnswer(i->i.getArgument(0));LeadTask result=service.claim(taskId);assertThat(result.getStatus()).isEqualTo("ASSIGNED");assertThat(result.getAssignedTo()).isEqualTo("agent-1");assertThat(result.getReservationExpiresAt()).isAfter(Instant.now());assertThat(lead.getAssignedTo()).isEqualTo("agent-1");}
 @Test void rejectsClaimWhenLimitReached(){when(tasks.countByTenantIdAndAssignedToAndStatus("tenant-a","agent-1","ASSIGNED")).thenReturn(5L);assertThatThrownBy(()->service.claim(taskId)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("limit");verify(tasks,never()).findScoped(any(),any());}
 @Test void rejectsCompletionByAnotherUser(){LeadTask t=open();t.setStatus("ASSIGNED");t.setAssignedTo("agent-2");when(tasks.findScoped(taskId,"tenant-a")).thenReturn(Optional.of(t));assertThatThrownBy(()->service.complete(taskId,new LeadTaskCompleteRequest("OTHER",null))).isInstanceOf(ResponseStatusException.class).hasMessageContaining("another user");}
 @Test void completesAssignedTask(){LeadTask t=open();t.setStatus("ASSIGNED");t.setAssignedTo("agent-1");when(tasks.findScoped(taskId,"tenant-a")).thenReturn(Optional.of(t));when(tasks.save(any())).thenAnswer(i->i.getArgument(0));LeadTask result=service.complete(taskId,new LeadTaskCompleteRequest("WHATSAPP_REPLIED","feito"));assertThat(result.getStatus()).isEqualTo("COMPLETED");assertThat(result.getOutcome()).isEqualTo("WHATSAPP_REPLIED");assertThat(result.getCompletedAt()).isNotNull();}
 private LeadTask open(){LeadTask t=new LeadTask();t.setLead(lead);t.setTenantId("tenant-a");t.setTitle("Responder");t.setTaskType("WHATSAPP_REPLY");t.setDueAt(Instant.now());return t;}
}
