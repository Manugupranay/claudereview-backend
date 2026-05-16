package com.claudereview.backend.dto;

/**
 * Categories of findings the reviewer can produce.
 * These map to common production-readiness concerns and are
 * intentionally limited — Claude must classify findings into
 * one of these buckets, not invent new ones.
 */
public enum Category {
    SECURITY,
    RELIABILITY,
    PERFORMANCE,
    MAINTAINABILITY,
    STYLE,
    TESTING,
    OBSERVABILITY
}