package com.bhoomi.land;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LandParcelRepository extends JpaRepository<LandParcel, UUID> {

    Optional<LandParcel> findByLocationIdAndSurveyNumberAndSubdivisionNumber(
            UUID locationId, String surveyNumber, String subdivisionNumber);
}
