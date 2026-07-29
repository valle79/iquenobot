package com.iquenobot.ai.interfaces.controller;

import com.iquenobot.ai.domain.dto.WhatsAppWebhookDto;
import com.iquenobot.orchestrator.application.WhatsAppWebhookAdapter;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.shared.domain.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WhatsApp Webhook", description = "Webhook endpoints for receiving WhatsApp messages from Evolution API")
public class WhatsAppWebhookController {

    private final WhatsAppWebhookAdapter webhookAdapter;

    @PostMapping("/{instanceId}")
    @Operation(
            summary = "Recibir webhook de WhatsApp",
            description = "Endpoint público para recibir mensajes entrantes desde Evolution API. " +
                    "El Conversation Orchestrator decide automáticamente la acción a tomar."
    )
    public ResponseEntity<ApiResponse<Void>> receiveWebhook(
            @PathVariable String instanceId,
            @Valid @RequestBody WhatsAppWebhookDto payload) {

        log.info("=== WEBHOOK WHATSAPP RECIBIDO ===");
        log.info("Instance: {} Event: {}", instanceId, payload.getEvent());

        log.info("Payload - event: {}, instanceId: {}, from: {}, messageId: {}, type: {}, text: {}",
                payload.getEvent(),
                payload.getInstanceId(),
                payload.getFrom(),
                payload.getMessageId(),
                payload.getType(),
                payload.getText());

        // -------------------------------------------------------------
        // Compatibilidad: data puede venir como OBJETO o ARRAY
        // -------------------------------------------------------------
        Object rawData = payload.getData();

        String dataKeys = "N/A";

        if (rawData instanceof Map<?, ?> map) {
            dataKeys = map.keySet().toString();
        } else if (rawData instanceof List<?> list) {

            if (!list.isEmpty() && list.get(0) instanceof Map<?, ?> map) {
                dataKeys = map.keySet().toString() + " (from array)";
            } else {
                dataKeys = "ARRAY(size=" + list.size() + ")";
            }
        }

        log.info("Payload - data present: {}, data keys: {}",
                rawData != null ? "SI" : "NO",
                dataKeys);

        try {
            ProcessingResult result = webhookAdapter.processWebhook(instanceId, payload);

            if (result.isSuccess()) {
                log.info("=== WEBHOOK PROCESADO EXITOSAMENTE ===");
            } else {
                log.warn("Webhook processing returned error: code={} message={}",
                        result.getErrorCode(), result.getMessage());
            }

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok(ApiResponse.success(null, "Webhook recibido"));
    }
}
