package com.claudereview.backend.dto;

/**
 * Confidence level Claude assigns to each finding.
 * Lets downstream consumers calibrate trust — e.g., auto-accept
 * HIGH security findings but human-review LOW style findings.
 */
public enum Confidence {
    HIGH,
    MEDIUM,
    LOW
}