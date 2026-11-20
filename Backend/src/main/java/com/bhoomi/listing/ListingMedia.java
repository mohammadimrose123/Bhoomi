package com.bhoomi.listing;

import java.util.UUID;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "listing_media")
public class ListingMedia extends BaseEntity {

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Column(name = "media_type", nullable = false, length = 40)
    private String mediaType;

    @Column(length = 300)
    private String caption;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ListingMedia() {
    }

    public ListingMedia(UUID listingId, String storageKey, String mediaType, String caption, int sortOrder) {
        this.listingId = listingId;
        this.storageKey = storageKey;
        this.mediaType = mediaType;
        this.caption = caption;
        this.sortOrder = sortOrder;
    }

    public UUID getListingId() { return listingId; }
    public String getStorageKey() { return storageKey; }
    public String getMediaType() { return mediaType; }
    public String getCaption() { return caption; }
    public int getSortOrder() { return sortOrder; }
}
