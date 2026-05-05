package com.backend.nmcomputercare.utils;

import lombok.Getter;

import java.time.LocalDateTime;

public class ExceptionHandlerReporter {

    @Getter
    private static String resolveIssueDetails;

    @Getter
    private static LocalDateTime exceptionDate;

    public static void setResolveIssueDetails(String resolveIssueDetails) {
        ExceptionHandlerReporter.resolveIssueDetails = resolveIssueDetails;
    }

    public static void setExceptionDate(LocalDateTime exceptionDate) {
        ExceptionHandlerReporter.exceptionDate = exceptionDate;
    }
}