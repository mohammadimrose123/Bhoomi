ALTER TABLE land_parcels
    ADD COLUMN latitude NUMERIC(9, 6),
    ADD COLUMN longitude NUMERIC(9, 6),
    ADD CONSTRAINT ck_land_parcels_coordinate_pair
        CHECK ((latitude IS NULL) = (longitude IS NULL)),
    ADD CONSTRAINT ck_land_parcels_latitude
        CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT ck_land_parcels_longitude
        CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180);

ALTER TABLE property_listings
    ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'BROKER_SUBMITTED',
    ADD COLUMN source_reference VARCHAR(500),
    ADD COLUMN last_availability_confirmed_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_property_listings_source_type
        CHECK (source_type IN ('BROKER_SUBMITTED', 'MANUAL_EVIDENCE', 'AUTHORIZED_EXTERNAL'));

DO $$
BEGIN
    IF EXISTS (
        SELECT land_parcel_id
        FROM property_listings
        WHERE status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'PUBLISHED', 'REJECTED')
        GROUP BY land_parcel_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Resolve duplicate active listings per parcel before applying V4';
    END IF;
END
$$;

CREATE UNIQUE INDEX uq_property_listings_active_parcel
    ON property_listings (land_parcel_id)
    WHERE status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'PUBLISHED', 'REJECTED');
