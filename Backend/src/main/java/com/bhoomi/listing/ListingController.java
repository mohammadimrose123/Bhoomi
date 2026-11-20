package com.bhoomi.listing;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bhoomi.land.LandMeasurementUnit;
import com.bhoomi.land.LandType;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public ListingDtos.PageResponse<ListingDtos.ListingResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String mandal,
            @RequestParam(required = false) String village,
            @RequestParam(required = false) LandType landType,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minExtent,
            @RequestParam(required = false) BigDecimal maxExtent,
            @RequestParam(required = false) LandMeasurementUnit measurementUnit) {
        return listingService.search(page, size, sortBy, direction, district, mandal, village,
                landType, minPrice, maxPrice, minExtent, maxExtent, measurementUnit);
    }

    @GetMapping("/{id}")
    public ListingDtos.ListingResponse get(@PathVariable UUID id) {
        return listingService.getPublicListing(id);
    }
}
