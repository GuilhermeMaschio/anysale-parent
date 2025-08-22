package com.anysale.lead.adapters.in.rest.query;

import com.anysale.lead.adapters.in.rest.dto.LeadResponseDto;
import com.anysale.lead.adapters.in.rest.maper.LeadMapper;
import com.anysale.lead.aplication.LeadService;
import com.anysale.lead.domain.model.Lead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/leads")
public class LeadQueryController {

    private final LeadService service;
    public LeadQueryController(LeadService service) { this.service = service; }

    @GetMapping("/{id}")
    public LeadResponseDto get(@PathVariable UUID id) {
        return LeadMapper.toResponse(service.get(id));
    }

    @GetMapping
    public Page<LeadResponseDto> list(
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        Sort s = parseSort(sort);
        Page<Lead> res = service.list(stage, q, page, size, s);
        return res.map(LeadMapper::toResponse);
    }

    private Sort parseSort(String sort) {
        // aceita "createdAt,DESC" ou "name,ASC"; default DESC se não vier direção
        String[] parts = sort.split(",", 2);
        if (parts.length == 2) {
            return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        }
        return Sort.by(Sort.Direction.DESC, parts[0]);
    }
}
