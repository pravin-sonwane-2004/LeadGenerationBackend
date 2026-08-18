package com.btechloanwala.LeadGeneration.enums;

/**
 * Lifecycle status of a lead.
 *
 * <p>New records are always created as {@link #NEW} by the backend. The internal
 * system later transitions them NEW &rarr; CONTACTED &rarr; CLOSED. Stored in the
 * database as a String (never as an ordinal number).</p>
 */
public enum LeadStatus {
    NEW,
    CONTACTED,
    CLOSED
}