package com.iquenobot.orchestrator.interfaces.controller;

import com.iquenobot.orchestrator.application.channel.EmailWebhookAdapter;
import com.iquenobot.orchestrator.application.channel.InstagramWebhookAdapter;
import com.iquenobot.orchestrator.application.channel.MessengerWebhookAdapter;
import com.iquenobot.orchestrator.application.channel.SmsWebhookAdapter;
import com.iquenobot.orchestrator.application.channel.WebchatWebhookAdapter;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.interfaces.dto.EmailWebhookDto;
import com.iquenobot.orchestrator.interfaces.dto.InstagramWebhookDto;
import com.iquenobot.orchestrator.interfaces.dto.MessengerWebhookDto;
import com.iquenobot.orchestrator.interfaces.dto.SmsWebhookDto;
import com.iquenobot.orchestrator.interfaces.dto.WebchatWebhookDto;
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

@RestController
@RequestMapping("/api/v1/channel")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Multi-Channel Webhook", description = "Webhook endpoints for multiple messaging channels")
public class ChannelWebhookController {

    private final MessengerWebhookAdapter messengerAdapter;
    private final InstagramWebhookAdapter instagramAdapter;
    private final EmailWebhookAdapter emailAdapter;
    private final WebchatWebhookAdapter webchatAdapter;
    private final SmsWebhookAdapter smsAdapter;

    @PostMapping("/messenger/{instanceId}")
    @Operation(summary = "Recibir webhook de Messenger")
    public ResponseEntity<ApiResponse<Void>> receiveMessenger(
            @PathVariable String instanceId,
            @Valid @RequestBody MessengerWebhookDto payload) {
        return process("messenger", instanceId, messengerAdapter.processWebhook(instanceId, payload));
    }

    @PostMapping("/instagram/{instanceId}")
    @Operation(summary = "Recibir webhook de Instagram")
    public ResponseEntity<ApiResponse<Void>> receiveInstagram(
            @PathVariable String instanceId,
            @Valid @RequestBody InstagramWebhookDto payload) {
        return process("instagram", instanceId, instagramAdapter.processWebhook(instanceId, payload));
    }

    @PostMapping("/email/{instanceId}")
    @Operation(summary = "Recibir webhook de Email")
    public ResponseEntity<ApiResponse<Void>> receiveEmail(
            @PathVariable String instanceId,
            @Valid @RequestBody EmailWebhookDto payload) {
        return process("email", instanceId, emailAdapter.processWebhook(instanceId, payload));
    }

    @PostMapping("/webchat/{instanceId}")
    @Operation(summary = "Recibir webhook de Webchat")
    public ResponseEntity<ApiResponse<Void>> receiveWebchat(
            @PathVariable String instanceId,
            @Valid @RequestBody WebchatWebhookDto payload) {
        return process("webchat", instanceId, webchatAdapter.processWebhook(instanceId, payload));
    }

    @PostMapping("/sms/{instanceId}")
    @Operation(summary = "Recibir webhook de SMS")
    public ResponseEntity<ApiResponse<Void>> receiveSms(
            @PathVariable String instanceId,
            @Valid @RequestBody SmsWebhookDto payload) {
        return process("sms", instanceId, smsAdapter.processWebhook(instanceId, payload));
    }

    private ResponseEntity<ApiResponse<Void>> process(String channel, String instanceId, ProcessingResult result) {
        log.info("Channel webhook processed: channel={} instance={} success={}", channel, instanceId, result.isSuccess());

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed successfully"));
        }

        log.warn("Webhook processing error for channel={}: {}", channel, result.getMessage());
        return ResponseEntity.ok(ApiResponse.success(null, "Webhook processed with warnings"));
    }
}
