package com.example.Media.shared.enums;

/**
 * Represents the business domain to which a media asset belongs.
 *
 * <p>Used to categorize and segregate media files across different feature areas,
 * enabling domain-scoped queries, storage quota enforcement, and billing ledger
 * segmentation. This enum eliminates the primitive obsession anti-pattern of
 * using raw {@code String} domain values throughout the service.</p>
 *
 * <p>MongoDB will persist these values as their {@link #name()} string (e.g., "GALLERY"),
 * ensuring full backward compatibility with any existing documents in the collection.</p>
 */
public enum MediaDomain {

    /** Media belonging to a user's personal photo/video gallery. */
    GALLERY,

    /** Media attached to event-related content (cover photos, attachments, etc.). */
    EVENTS,

    /** Media used as a user's profile avatar. */
    PROFILE_AVATAR,

    /** Media sent or received within the messaging feature. */
    MESSAGING
}
