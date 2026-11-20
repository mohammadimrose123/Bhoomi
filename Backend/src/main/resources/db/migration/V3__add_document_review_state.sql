ALTER TABLE land_documents
    ADD COLUMN review_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN reviewed_by UUID,
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN review_note VARCHAR(1000);

ALTER TABLE land_documents
    ADD CONSTRAINT fk_land_documents_reviewer
        FOREIGN KEY (reviewed_by) REFERENCES users (id),
    ADD CONSTRAINT ck_land_documents_review_status
        CHECK (review_status IN ('PENDING', 'REVIEWED', 'REJECTED'));

CREATE INDEX ix_land_documents_review ON land_documents (review_status, created_at);
