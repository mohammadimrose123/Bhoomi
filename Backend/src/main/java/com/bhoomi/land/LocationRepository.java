package com.bhoomi.land;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    Optional<Location> findByCountryAndStateAndDistrictAndMandalAndVillage(
            String country, String state, String district, String mandal, String village);
}
