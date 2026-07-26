package com.iquenobot.contact.interfaces.mapper;

import com.iquenobot.contact.domain.dto.ContactDto;
import com.iquenobot.contact.domain.dto.CreateContactRequestDto;
import com.iquenobot.contact.domain.entity.Contact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ContactMapper {

    @Mapping(target = "displayName", expression = "java(contact.getDisplayName())")
    ContactDto toDto(Contact contact);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "conversationCount", constant = "0")
    @Mapping(target = "messageCount", constant = "0")
    @Mapping(target = "subscribed", constant = "true")
    @Mapping(target = "lastContactedAt", ignore = true)
    @Mapping(target = "unsubscribedAt", ignore = true)
    @Mapping(target = "blockedAt", ignore = true)
    @Mapping(target = "blockedReason", ignore = true)
    @Mapping(target = "customFields", ignore = true)
    @Mapping(target = "conversations", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Contact toEntity(CreateContactRequestDto dto);
}