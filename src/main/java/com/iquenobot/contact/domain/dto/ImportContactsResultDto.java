package com.iquenobot.contact.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportContactsResultDto {
    private int total;
    private int created;
    private int skipped;
    private int errors;
    private List<String> messages;
}
