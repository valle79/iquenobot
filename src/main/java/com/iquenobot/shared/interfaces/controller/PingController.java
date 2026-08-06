package com.iquenobot.shared.interfaces.controller;

import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ping")
@Tag(name = "Sistema", description = "Endpoint de mantenimiento (health check ligero)")
public class PingController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ping", description = "Heartbeat de bajo consumo para mantener el servicio activo. Sin acceso a base de datos.")
    public ResponseEntity<ApiResponse<Map<String, String>>> ping() {
        return ResponseEntity
                .ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate")
                .body(ApiResponse.success(Map.of("status", "up"), "pong"));
    }
}