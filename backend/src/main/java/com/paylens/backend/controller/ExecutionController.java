package com.paylens.backend.controller;

import com.paylens.backend.dto.*;
import com.paylens.backend.service.ExecutionService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/executions")
    public ResponseEntity<ExecutionResponse> execute(@Valid @RequestBody ExecutionApiRequest request) {
        try {
            ExecutionResponse response = executionService.execute(request);
            HttpStatus status = switch (response.status()) {
                case ELIGIBILITY_REJECTED -> HttpStatus.UNPROCESSABLE_ENTITY;
                case FAILED -> HttpStatus.PAYMENT_REQUIRED;
                case UNSUPPORTED_EXECUTION -> HttpStatus.NOT_IMPLEMENTED;
                case UNKNOWN -> HttpStatus.GATEWAY_TIMEOUT;
                default -> HttpStatus.OK;
            };
            return ResponseEntity.status(status).body(response);
        } catch (DataIntegrityViolationException e) {
            // Concurrent duplicate idempotency key collision handled at database unique constraint level
            ExecutionResponse existing = executionService.getExecutionByIdempotencyKey(request.idempotencyKey());
            return ResponseEntity.ok(existing);
        }
    }

    @GetMapping("/executions")
    public ResponseEntity<Map<String, List<ExecutionSummaryResponse>>> listExecutions(
            @RequestParam(required = false) String status) {
        List<ExecutionSummaryResponse> executions = executionService.listExecutions(status);
        return ResponseEntity.ok(Map.of("executions", executions));
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ExecutionResponse> getExecution(@PathVariable String executionId) {
        ExecutionResponse response = executionService.getExecution(executionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/decisions/{decisionId}/execution")
    public ResponseEntity<ExecutionResponse> getExecutionByDecisionId(@PathVariable String decisionId) {
        ExecutionResponse response = executionService.getExecutionByDecisionId(decisionId);
        return ResponseEntity.ok(response);
    }
}
