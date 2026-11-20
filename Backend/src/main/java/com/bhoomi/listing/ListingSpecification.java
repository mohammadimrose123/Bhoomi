package com.bhoomi.listing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.bhoomi.land.LandMeasurementUnit;
import com.bhoomi.land.LandParcel;
import com.bhoomi.land.LandType;
import com.bhoomi.land.Location;

import jakarta.persistence.criteria.Predicate;

public final class ListingSpecification {

    private ListingSpecification() {
    }

    public static Specification<PropertyListing> publishedSearch(
            String district, String mandal, String village, LandType landType,
            BigDecimal minPrice, BigDecimal maxPrice, BigDecimal minExtent, BigDecimal maxExtent,
            LandMeasurementUnit measurementUnit) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("status"), ListingStatus.PUBLISHED));
            if (minPrice != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("askingPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("askingPrice"), maxPrice));
            }
            if (district != null || mandal != null || village != null || landType != null
                    || minExtent != null || maxExtent != null || measurementUnit != null) {
                var parcelQuery = query.subquery(java.util.UUID.class);
                var parcel = parcelQuery.from(LandParcel.class);
                parcelQuery.select(parcel.get("id"));
                List<Predicate> parcelPredicates = new ArrayList<>();
                if (landType != null) {
                    parcelPredicates.add(builder.equal(parcel.get("landType"), landType));
                }
                if (minExtent != null) {
                    parcelPredicates.add(builder.greaterThanOrEqualTo(parcel.get("extent"), minExtent));
                }
                if (maxExtent != null) {
                    parcelPredicates.add(builder.lessThanOrEqualTo(parcel.get("extent"), maxExtent));
                }
                if (measurementUnit != null) {
                    parcelPredicates.add(builder.equal(parcel.get("measurementUnit"), measurementUnit));
                }
                if (district != null || mandal != null || village != null) {
                    var locationQuery = parcelQuery.subquery(java.util.UUID.class);
                    var location = locationQuery.from(Location.class);
                    locationQuery.select(location.get("id"));
                    List<Predicate> locationPredicates = new ArrayList<>();
                    if (district != null) {
                        locationPredicates.add(builder.equal(
                                builder.lower(location.get("district")), district.toLowerCase(java.util.Locale.ROOT)));
                    }
                    if (mandal != null) {
                        locationPredicates.add(builder.equal(
                                builder.lower(location.get("mandal")), mandal.toLowerCase(java.util.Locale.ROOT)));
                    }
                    if (village != null) {
                        locationPredicates.add(builder.equal(
                                builder.lower(location.get("village")), village.toLowerCase(java.util.Locale.ROOT)));
                    }
                    locationQuery.where(locationPredicates.toArray(Predicate[]::new));
                    parcelPredicates.add(parcel.get("locationId").in(locationQuery));
                }
                parcelQuery.where(parcelPredicates.toArray(Predicate[]::new));
                predicates.add(root.get("landParcelId").in(parcelQuery));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
