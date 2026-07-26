package com.iquenobot.chatbot.interfaces.mapper;

import com.iquenobot.chatbot.domain.dto.ChatbotFlowDto;
import com.iquenobot.chatbot.domain.dto.ChatbotIntentDto;
import com.iquenobot.chatbot.domain.dto.CreateChatbotFlowRequestDto;
import com.iquenobot.chatbot.domain.dto.CreateChatbotIntentRequestDto;
import com.iquenobot.chatbot.domain.entity.ChatbotFlow;
import com.iquenobot.chatbot.domain.entity.ChatbotIntent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ChatbotMapper {

    ChatbotIntentDto toIntentDto(ChatbotIntent intent);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "matchedCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ChatbotIntent toIntent(CreateChatbotIntentRequestDto request);

    ChatbotFlowDto toFlowDto(ChatbotFlow flow);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "successCount", ignore = true)
    @Mapping(target = "failureCount", ignore = true)
    @Mapping(target = "executionCount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    ChatbotFlow toFlow(CreateChatbotFlowRequestDto request);
}
