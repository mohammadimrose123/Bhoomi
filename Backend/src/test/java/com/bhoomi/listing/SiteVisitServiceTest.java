package com.bhoomi.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;

class SiteVisitServiceTest {

    private SiteVisitRepository siteVisitRepository;
    private PropertyListingRepository listingRepository;
    private BrokerProfileRepository brokerRepository;
    private SiteVisitService service;

    @BeforeEach
    void setUp() {
        siteVisitRepository = mock(SiteVisitRepository.class);
        listingRepository = mock(PropertyListingRepository.class);
        brokerRepository = mock(BrokerProfileRepository.class);
        service = new SiteVisitService(siteVisitRepository, listingRepository, brokerRepository);
    }

    @Test
    void createStoresTheBuyerRequestedFutureTime() {
        UUID buyerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        Instant requestedAt = Instant.now().plusSeconds(3600);
        PropertyListing listing = publishedListing(listingId, UUID.randomUUID());
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(siteVisitRepository.save(org.mockito.ArgumentMatchers.any(SiteVisit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SiteVisitDtos.SiteVisitResponse response = service.create(buyerId, listingId,
                new SiteVisitDtos.SiteVisitRequest(requestedAt, "Please confirm"));

        assertThat(response.requestedAt()).isEqualTo(requestedAt);
        assertThat(response.status()).isEqualTo(SiteVisitStatus.REQUESTED);
    }

    @Test
    void createRejectsPastRequestedTime() {
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), UUID.randomUUID(),
                new SiteVisitDtos.SiteVisitRequest(Instant.now().minusSeconds(1), "Visit")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        verify(listingRepository, never()).findById(org.mockito.ArgumentMatchers.any());
        verify(siteVisitRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void brokerCanConfirmAndRescheduleAnOwnedRequest() {
        UUID brokerUserId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        Instant requestedAt = Instant.now().plusSeconds(3600);
        BrokerProfile profile = new BrokerProfile(brokerUserId, "Bhoomi Realty");
        ReflectionTestUtils.setField(profile, "id", brokerProfileId);
        PropertyListing listing = publishedListing(listingId, brokerProfileId);
        SiteVisit visit = new SiteVisit(listingId, UUID.randomUUID(), Instant.now().plusSeconds(60),
                SiteVisitStatus.REQUESTED, "Initial");
        ReflectionTestUtils.setField(visit, "id", visitId);
        when(brokerRepository.findByUserId(brokerUserId)).thenReturn(Optional.of(profile));
        when(siteVisitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(siteVisitRepository.save(visit)).thenReturn(visit);

        SiteVisitDtos.SiteVisitResponse response = service.updateBrokerStatus(brokerUserId, visitId,
                new SiteVisitDtos.SiteVisitStatusRequest(SiteVisitStatus.CONFIRMED, requestedAt, "Confirmed"));

        assertThat(response.status()).isEqualTo(SiteVisitStatus.CONFIRMED);
        assertThat(response.requestedAt()).isEqualTo(requestedAt);
        assertThat(response.notes()).isEqualTo("Confirmed");
    }

    @Test
    void brokerCannotManageAnotherBrokersVisit() {
        UUID brokerUserId = UUID.randomUUID();
        UUID ownBrokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        BrokerProfile profile = new BrokerProfile(brokerUserId, "Own brokerage");
        ReflectionTestUtils.setField(profile, "id", ownBrokerProfileId);
        SiteVisit visit = new SiteVisit(listingId, UUID.randomUUID(), Instant.now().plusSeconds(60),
                SiteVisitStatus.REQUESTED, "Visit");
        ReflectionTestUtils.setField(visit, "id", visitId);
        when(brokerRepository.findByUserId(brokerUserId)).thenReturn(Optional.of(profile));
        when(siteVisitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(publishedListing(listingId, UUID.randomUUID())));

        assertThatThrownBy(() -> service.updateBrokerStatus(brokerUserId, visitId,
                new SiteVisitDtos.SiteVisitStatusRequest(SiteVisitStatus.CONFIRMED, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verify(siteVisitRepository, never()).save(visit);
    }

    @Test
    void completedVisitCannotBeReopened() {
        UUID brokerUserId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        BrokerProfile profile = new BrokerProfile(brokerUserId, "Bhoomi Realty");
        ReflectionTestUtils.setField(profile, "id", brokerProfileId);
        SiteVisit visit = new SiteVisit(listingId, UUID.randomUUID(), Instant.now().plusSeconds(60),
                SiteVisitStatus.COMPLETED, "Done");
        ReflectionTestUtils.setField(visit, "id", visitId);
        when(brokerRepository.findByUserId(brokerUserId)).thenReturn(Optional.of(profile));
        when(siteVisitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(publishedListing(listingId, brokerProfileId)));

        assertThatThrownBy(() -> service.updateBrokerStatus(brokerUserId, visitId,
                new SiteVisitDtos.SiteVisitStatusRequest(SiteVisitStatus.CONFIRMED, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
        verify(siteVisitRepository, never()).save(visit);
    }

    private static PropertyListing publishedListing(UUID listingId, UUID brokerProfileId) {
        PropertyListing listing = new PropertyListing(UUID.randomUUID(), brokerProfileId,
                "Listing", "Description", java.math.BigDecimal.valueOf(100000));
        listing.setStatus(ListingStatus.PUBLISHED, UUID.randomUUID(), "Approved");
        ReflectionTestUtils.setField(listing, "id", listingId);
        return listing;
    }
}
