package com.backend.nmcomputercare.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class ErrorResponse implements ResponseContract {
    @JsonProperty("IssueMessage")
    private String resolveIssueResponse;
    @JsonProperty("issueDate")
    private LocalDateTime errorOccurredDate;
}
