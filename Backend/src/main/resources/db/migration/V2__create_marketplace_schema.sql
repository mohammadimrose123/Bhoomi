CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(32),
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'BUYER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT ck_users_role CHECK (role IN ('BUYER', 'BROKER', 'ADMIN')),
    CONSTRAINT ck_users_contact CHECK (email <> '' OR phone IS NOT NULL)
);
CREATE UNIQUE INDEX uq_users_email_ci ON users (lower(email));

CREATE TABLE broker_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    business_name VARCHAR(160) NOT NULL,
    license_number VARCHAR(100),
    approval_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_broker_profiles_user UNIQUE (user_id),
    CONSTRAINT uq_broker_profiles_license UNIQUE (license_number),
    CONSTRAINT fk_broker_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_broker_profiles_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE locations (
    id UUID PRIMARY KEY,
    country VARCHAR(80) NOT NULL DEFAULT 'India',
    state VARCHAR(100) NOT NULL,
    district VARCHAR(120) NOT NULL,
    mandal VARCHAR(120) NOT NULL,
    village VARCHAR(120) NOT NULL,
    postal_code VARCHAR(16),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_locations_address UNIQUE (country, state, district, mandal, village)
);
CREATE INDEX ix_locations_district_mandal_village
    ON locations (lower(district), lower(mandal), lower(village));

CREATE TABLE land_parcels (
    id UUID PRIMARY KEY,
    location_id UUID NOT NULL,
    survey_number VARCHAR(80) NOT NULL,
    subdivision_number VARCHAR(80) NOT NULL DEFAULT '',
    extent NUMERIC(18, 4) NOT NULL,
    measurement_unit VARCHAR(24) NOT NULL,
    land_type VARCHAR(40) NOT NULL,
    boundary geometry(MultiPolygon, 4326),
    boundary_is_approximate BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_land_parcels_location FOREIGN KEY (location_id) REFERENCES locations (id),
    CONSTRAINT uq_land_parcel_identity UNIQUE (location_id, survey_number, subdivision_number),
    CONSTRAINT ck_land_parcels_extent CHECK (extent > 0),
    CONSTRAINT ck_land_parcels_unit CHECK (measurement_unit IN ('ACRE', 'SQUARE_YARD', 'SQUARE_FOOT', 'HECTARE', 'GUNTA')),
    CONSTRAINT ck_land_parcels_type CHECK (land_type IN ('AGRICULTURAL', 'RESIDENTIAL', 'COMMERCIAL', 'INDUSTRIAL', 'MIXED_USE', 'OTHER'))
);
CREATE INDEX ix_land_parcels_location ON land_parcels (location_id);
CREATE INDEX ix_land_parcels_survey ON land_parcels (survey_number, subdivision_number);
CREATE INDEX ix_land_parcels_type_extent ON land_parcels (land_type, extent, measurement_unit);
CREATE INDEX ix_land_parcels_boundary ON land_parcels USING GIST (boundary);

CREATE TABLE property_listings (
    id UUID PRIMARY KEY,
    land_parcel_id UUID NOT NULL,
    broker_profile_id UUID NOT NULL,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    asking_price NUMERIC(18, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    reviewed_by UUID,
    review_note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_property_listings_parcel FOREIGN KEY (land_parcel_id) REFERENCES land_parcels (id),
    CONSTRAINT fk_property_listings_broker FOREIGN KEY (broker_profile_id) REFERENCES broker_profiles (id),
    CONSTRAINT fk_property_listings_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
    CONSTRAINT ck_property_listings_price CHECK (asking_price > 0),
    CONSTRAINT ck_property_listings_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'PUBLISHED', 'REJECTED', 'SOLD', 'UNAVAILABLE', 'ARCHIVED')),
    CONSTRAINT ck_property_listings_published CHECK (status <> 'PUBLISHED' OR published_at IS NOT NULL),
    CONSTRAINT ck_property_listings_submitted CHECK (
        status NOT IN ('SUBMITTED', 'UNDER_REVIEW', 'PUBLISHED', 'REJECTED', 'SOLD', 'UNAVAILABLE', 'ARCHIVED')
        OR submitted_at IS NOT NULL
    )
);
CREATE INDEX ix_property_listings_public ON property_listings (status, created_at DESC);
CREATE INDEX ix_property_listings_price ON property_listings (status, asking_price);
CREATE INDEX ix_property_listings_broker ON property_listings (broker_profile_id, created_at DESC);
CREATE INDEX ix_property_listings_review ON property_listings (status, submitted_at);

CREATE TABLE listing_media (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    media_type VARCHAR(40) NOT NULL,
    caption VARCHAR(300),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_listing_media_listing FOREIGN KEY (listing_id) REFERENCES property_listings (id) ON DELETE CASCADE,
    CONSTRAINT uq_listing_media_storage UNIQUE (storage_key),
    CONSTRAINT ck_listing_media_sort CHECK (sort_order >= 0)
);
CREATE INDEX ix_listing_media_listing ON listing_media (listing_id, sort_order);

CREATE TABLE land_documents (
    id UUID PRIMARY KEY,
    land_parcel_id UUID NOT NULL,
    uploaded_by UUID NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    provenance VARCHAR(40) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_land_documents_parcel FOREIGN KEY (land_parcel_id) REFERENCES land_parcels (id),
    CONSTRAINT fk_land_documents_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id),
    CONSTRAINT uq_land_documents_storage UNIQUE (storage_key),
    CONSTRAINT ck_land_documents_provenance CHECK (provenance IN ('BROKER_SUBMITTED', 'INDEPENDENTLY_REVIEWED'))
);
CREATE INDEX ix_land_documents_parcel ON land_documents (land_parcel_id);

CREATE TABLE verification_checks (
    id UUID PRIMARY KEY,
    land_parcel_id UUID NOT NULL,
    checked_by UUID NOT NULL,
    check_type VARCHAR(80) NOT NULL,
    status VARCHAR(24) NOT NULL,
    evidence_reference VARCHAR(500),
    notes VARCHAR(2000),
    checked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verification_checks_parcel FOREIGN KEY (land_parcel_id) REFERENCES land_parcels (id),
    CONSTRAINT fk_verification_checks_user FOREIGN KEY (checked_by) REFERENCES users (id),
    CONSTRAINT ck_verification_checks_status CHECK (status IN ('NOT_ASSESSED', 'PENDING', 'SUPPORTED', 'DISPUTED', 'INCONCLUSIVE'))
);
CREATE INDEX ix_verification_checks_parcel ON verification_checks (land_parcel_id, checked_at DESC);

CREATE TABLE enquiries (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL,
    buyer_id UUID NOT NULL,
    message VARCHAR(2000) NOT NULL,
    contact_phone VARCHAR(32),
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_enquiries_listing FOREIGN KEY (listing_id) REFERENCES property_listings (id),
    CONSTRAINT fk_enquiries_buyer FOREIGN KEY (buyer_id) REFERENCES users (id),
    CONSTRAINT ck_enquiries_status CHECK (status IN ('OPEN', 'RESPONDED', 'CLOSED'))
);
CREATE INDEX ix_enquiries_listing ON enquiries (listing_id, created_at DESC);
CREATE INDEX ix_enquiries_buyer ON enquiries (buyer_id, created_at DESC);

CREATE TABLE favorites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    listing_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_listing FOREIGN KEY (listing_id) REFERENCES property_listings (id) ON DELETE CASCADE,
    CONSTRAINT uq_favorites_user_listing UNIQUE (user_id, listing_id)
);
CREATE INDEX ix_favorites_listing ON favorites (listing_id);

CREATE TABLE site_visits (
    id UUID PRIMARY KEY,
    listing_id UUID NOT NULL,
    buyer_id UUID NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'REQUESTED',
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_site_visits_listing FOREIGN KEY (listing_id) REFERENCES property_listings (id),
    CONSTRAINT fk_site_visits_buyer FOREIGN KEY (buyer_id) REFERENCES users (id),
    CONSTRAINT ck_site_visits_status CHECK (status IN ('REQUESTED', 'CONFIRMED', 'COMPLETED', 'CANCELLED'))
);
CREATE INDEX ix_site_visits_listing ON site_visits (listing_id, requested_at);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
);
CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_type, entity_id, created_at DESC);
CREATE INDEX ix_audit_logs_actor ON audit_logs (actor_user_id, created_at DESC);
