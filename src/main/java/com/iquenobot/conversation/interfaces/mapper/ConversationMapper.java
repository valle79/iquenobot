package com.iquenobot.conversation.interfaces.mapper;

import com.iquenobot.auth.interfaces.mapper.AuthMapper;
import com.iquenobot.contact.interfaces.mapper.ContactMapper;
import com.iquenobot.conversation.domain.dto.ConversationDto;
import com.iquenobot.conversation.domain.dto.ConversationMessageDto;
import com.iquenobot.conversation.domain.dto.MessageAttachmentDto;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.entity.MessageAttachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ContactMapper.class, AuthMapper.class})
public interface ConversationMapper {

    @Mapping(target = "lastMessage", ignore = true)
    ConversationDto toDto(Conversation conversation);

    @Mapping(target = "userName", source = "user.fullName")
    @Mapping(target = "attachments", source = "attachments")
    ConversationMessageDto toMessageDto(ConversationMessage message);

    @Mapping(target = "formattedFileSize", expression = "java(attachment.getFormattedFileSize())")
    MessageAttachmentDto toAttachmentDto(MessageAttachment attachment);
}