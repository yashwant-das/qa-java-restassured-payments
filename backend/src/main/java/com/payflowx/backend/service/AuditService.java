package com.payflowx.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflowx.backend.entity.AuditLogEntity;
import com.payflowx.backend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {
    private final AuditLogRepository repository;
    private final ObjectMapper mapper;

    public AuditService(AuditLogRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public void log(String apiName, Object request, Object response, String status) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setApiName(apiName);
        entity.setRequestPayload(write(request));
        entity.setResponsePayload(write(response));
        entity.setStatus(status);
        entity.setTimestamp(Instant.now());
        repository.save(entity);
    }

    private String write(Object value) {
        try {
            return value == null ? "{}" : mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"serialization\":\"failed\"}";
        }
    }
}
