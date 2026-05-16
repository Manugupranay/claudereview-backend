package com.claudereview.backend.dto;

/**
 * Severity levels for findings.
 * Order matters: declared from most to least severe.
 * Clients can compare ordinals if filtering by minimum severity.
 */
public enum Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}