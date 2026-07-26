package com.iquenobot.lead.interfaces.mapper;

import com.iquenobot.lead.domain.dto.CreateLeadRequestDto;
import com.iquenobot.lead.domain.dto.LeadDto;
import com.iquenobot.lead.domain.entity.Lead;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    @Mapping(target = "open", expression = "java(lead.isOpen())")
    @Mapping(target = "closed", expression = "java(lead.isClosed())")
    @Mapping(target = "daysSinceCreated", expression = "java(lead.getDaysSinceCreated())")
    @Mapping(target = "daysSinceLastContact", expression = "java(lead.getDaysSinceLastContact())")
    LeadDto toDto(Lead lead);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "contact", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "firstContactAt", ignore = true)
    @Mapping(target = "lastContactAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "convertedToContactId", ignore = true)
    @Mapping(target = "lostReason", ignore = true)
    @Mapping(target = "customFields", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    Lead toEntity(CreateLeadRequestDto dto);
}
