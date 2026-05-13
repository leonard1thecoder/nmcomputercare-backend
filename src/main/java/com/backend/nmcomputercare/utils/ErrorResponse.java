package com.backend.nmcomputercare.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.Data;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ErrorResponse implements ResponseContract {
    @JsonProperty("IssueMessage")
    private String resolveIssueResponse;
    @JsonProperty("issueDate")
    private LocalDateTime errorOccurredDate;
}
