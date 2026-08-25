package com.paylens.backend.controller;
import com.paylens.backend.service.AuditService; import java.util.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/audit") public class AuditController { private final AuditService service; public AuditController(AuditService service){this.service=service;} @GetMapping public Map<String,Object> list(@RequestParam(required=false) String decisionId){return Map.of("events",service.list(decisionId));} }
