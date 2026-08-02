package com.iquenobot.channel.interfaces.mapper;

import com.iquenobot.channel.domain.dto.CreateWhatsAppChannelRequestDto;
import com.iquenobot.channel.domain.dto.WhatsAppChannelDto;
import com.iquenobot.channel.domain.entity.WhatsAppChannel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WhatsAppChannelMapper {

    WhatsAppChannelDto toDto(WhatsAppChannel channel);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "webhookUrl", ignore = true)
    @Mapping(target = "connectedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    WhatsAppChannel toEntity(CreateWhatsAppChannelRequestDto dto);
}
