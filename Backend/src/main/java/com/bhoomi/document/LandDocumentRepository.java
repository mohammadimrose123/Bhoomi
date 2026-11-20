package com.bhoomi.document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LandDocumentRepository extends JpaRepository<LandDocument, UUID> {

    List<LandDocument> findByLandParcelIdAndUploadedByOrderByCreatedAtDesc(UUID landParcelId, UUID uploadedBy);

    List<LandDocument> findByReviewStatusOrderByCreatedAtAsc(DocumentReviewStatus reviewStatus);

    Optional<LandDocument> findByIdAndUploadedBy(UUID id, UUID uploadedBy);
}
