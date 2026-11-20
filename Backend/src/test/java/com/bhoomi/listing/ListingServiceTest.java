package com.bhoomi.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.listing.ListingDtos.AdminStatusRequest;
import com.bhoomi.common.AuditService;
import com.bhoomi.land.LandMeasurementUnit;
import com.bhoomi.land.LandParcel;
import com.bhoomi.land.LandParcelRepository;
import com.bhoomi.land.LocationRepository;
import com.bhoomi.land.LandType;
import com.bhoomi.land.Location;

import static org.mockito.Mockito.mock;

class ListingServiceTest {

    private PropertyListingRepository listingRepository;
    private BrokerProfileRepository brokerRepository;
    private LandParcelRepository parcelRepository;
    private LocationRepository locationRepository;
    private ListingMediaRepository mediaRepository;
    private ListingService listingService;

    @BeforeEach
    void setUp() {
        listingRepository = mock(PropertyListingRepository.class);
        brokerRepository = mock(BrokerProfileRepository.class);
        parcelRepository = mock(LandParcelRepository.class);
        locationRepository = mock(LocationRepository.class);
        mediaRepository = mock(ListingMediaRepository.class);
        listingService = new ListingService(listingRepository, parcelRepository,
                locationRepository, brokerRepository, mock(AuditService.class), mediaRepository);
    }

    @Test
    void brokerCannotUpdateAnotherBrokersListing() {
        UUID brokerUserId = UUID.randomUUID();
        UUID ownBrokerProfileId = UUID.randomUUID();
        UUID anotherBrokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BrokerProfile ownProfile = new BrokerProfile(brokerUserId, "Own brokerage");
        ReflectionTestUtils.setField(ownProfile, "id", ownBrokerProfileId);
        PropertyListing listing = new PropertyListing(UUID.randomUUID(), anotherBrokerProfileId,
                "Parcel", "Description", BigDecimal.valueOf(50000));
        ReflectionTestUtils.setField(listing, "id", listingId);
        when(brokerRepository.findByUserId(brokerUserId)).thenReturn(Optional.of(ownProfile));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.update(brokerUserId, listingId,
                new ListingDtos.UpdateRequest("Changed", "Changed details", BigDecimal.valueOf(60000))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(listingRepository, never()).save(any(PropertyListing.class));
    }

    @Test
    void brokerCanSubmitOwnDraftForReview() {
        UUID userId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID parcelId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Brokerage");
        ReflectionTestUtils.setField(broker, "id", brokerProfileId);
        broker.setApprovalStatus(com.bhoomi.broker.BrokerApprovalStatus.APPROVED);
        PropertyListing draft = new PropertyListing(parcelId, brokerProfileId,
                "Draft listing", "Details", BigDecimal.valueOf(500000));
        ReflectionTestUtils.setField(draft, "id", listingId);
        LandParcel parcel = new LandParcel(locationId, "100", "", BigDecimal.ONE,
                LandMeasurementUnit.ACRE, LandType.AGRICULTURAL);
        ReflectionTestUtils.setField(parcel, "id", parcelId);
        Location location = new Location("Telangana", "District", "Mandal", "Village", null);
        ReflectionTestUtils.setField(location, "id", locationId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(draft));
        when(listingRepository.save(draft)).thenReturn(draft);
        when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));

        ListingDtos.ListingResponse response = listingService.submit(userId, listingId);

        assertThat(response.status()).isEqualTo(ListingStatus.SUBMITTED);
        assertThat(draft.getSubmittedAt()).isNotNull();
        verify(listingRepository).save(draft);
    }

    @Test
    void pendingBrokerCanCreateDraftWithoutSubmittingIt() {
        UUID userId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID parcelId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Pending brokerage");
        ReflectionTestUtils.setField(broker, "id", brokerProfileId);
        Location location = new Location("Telangana", "District", "Mandal", "Village", null);
        ReflectionTestUtils.setField(location, "id", locationId);
        LandParcel parcel = new LandParcel(locationId, "100", "", BigDecimal.ONE,
                LandMeasurementUnit.ACRE, LandType.AGRICULTURAL);
        ReflectionTestUtils.setField(parcel, "id", parcelId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(locationRepository.findByCountryAndStateAndDistrictAndMandalAndVillage(
                "India", "Telangana", "District", "Mandal", "Village")).thenReturn(Optional.of(location));
        when(parcelRepository.findByLocationIdAndSurveyNumberAndSubdivisionNumber(locationId, "100", ""))
                .thenReturn(Optional.of(parcel));
        when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        doAnswer(invocation -> {
            PropertyListing saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        }).when(listingRepository).save(any(PropertyListing.class));

        ListingDtos.ListingResponse response = listingService.createDraft(userId,
                new ListingDtos.CreateRequest("Draft", "Details", BigDecimal.valueOf(500000),
                        new ListingDtos.ParcelRequest("Telangana", "District", "Mandal", "Village", "100", null,
                                BigDecimal.ONE, LandMeasurementUnit.ACRE, LandType.AGRICULTURAL)));

        assertThat(response.status()).isEqualTo(ListingStatus.DRAFT);
        verify(listingRepository).save(argThat(listing -> listing.getStatus() == ListingStatus.DRAFT));
    }

    @Test
    void pendingBrokerCannotSubmitDraft() {
        UUID userId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Pending brokerage");
        ReflectionTestUtils.setField(broker, "id", brokerProfileId);
        PropertyListing draft = new PropertyListing(UUID.randomUUID(), brokerProfileId,
                "Draft listing", "Details", BigDecimal.valueOf(500000));
        ReflectionTestUtils.setField(draft, "id", listingId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));

        assertThatThrownBy(() -> listingService.submit(userId, listingId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verify(listingRepository, never()).save(any(PropertyListing.class));
    }

    @Test
    void brokerCannotChangeAnotherBrokersListingAvailability() {
        UUID userId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Own brokerage");
        ReflectionTestUtils.setField(broker, "id", brokerProfileId);
        PropertyListing listing = new PropertyListing(UUID.randomUUID(), UUID.randomUUID(),
                "Published", "Details", BigDecimal.valueOf(500000));
        listing.setStatus(ListingStatus.PUBLISHED, UUID.randomUUID(), null);
        ReflectionTestUtils.setField(listing, "id", listingId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> listingService.changeBrokerStatus(userId, listingId, ListingStatus.SOLD))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(listingRepository, never()).save(any(PropertyListing.class));
    }

    @Test
    void duplicateActiveParcelCannotReceiveAnotherListing() {
        UUID userId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID parcelId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Brokerage");
        ReflectionTestUtils.setField(broker, "id", brokerProfileId);
        Location location = new Location("Telangana", "District", "Mandal", "Village", null);
        ReflectionTestUtils.setField(location, "id", locationId);
        LandParcel parcel = new LandParcel(locationId, "100", "", BigDecimal.ONE,
                LandMeasurementUnit.ACRE, LandType.AGRICULTURAL);
        ReflectionTestUtils.setField(parcel, "id", parcelId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(locationRepository.findByCountryAndStateAndDistrictAndMandalAndVillage(
                "India", "Telangana", "District", "Mandal", "Village")).thenReturn(Optional.of(location));
        when(parcelRepository.findByLocationIdAndSurveyNumberAndSubdivisionNumber(locationId, "100", ""))
                .thenReturn(Optional.of(parcel));
        when(listingRepository.existsByLandParcelIdAndStatusIn(eq(parcelId), any())).thenReturn(true);

        assertThatThrownBy(() -> listingService.createDraft(userId,
                new ListingDtos.CreateRequest("Duplicate", "Details", BigDecimal.valueOf(500000),
                        new ListingDtos.ParcelRequest("Telangana", "District", "Mandal", "Village", "100", null,
                                BigDecimal.ONE, LandMeasurementUnit.ACRE, LandType.AGRICULTURAL))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
        verify(listingRepository, never()).save(any(PropertyListing.class));
    }

    @Test
    void brokerCanConfirmAvailabilityOfOwnedPublishedListing() {
        UUID userId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID parcelId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Brokerage");
        ReflectionTestUtils.setField(broker, "id", brokerProfileId);
        PropertyListing listing = new PropertyListing(parcelId, brokerProfileId,
                "Published", "Details", BigDecimal.valueOf(500000));
        listing.setStatus(ListingStatus.PUBLISHED, UUID.randomUUID(), null);
        ReflectionTestUtils.setField(listing, "id", listingId);
        LandParcel parcel = new LandParcel(locationId, "100", "", BigDecimal.ONE,
                LandMeasurementUnit.ACRE, LandType.AGRICULTURAL);
        ReflectionTestUtils.setField(parcel, "id", parcelId);
        Location location = new Location("Telangana", "District", "Mandal", "Village", null);
        ReflectionTestUtils.setField(location, "id", locationId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(listing)).thenReturn(listing);
        when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));

        ListingDtos.ListingResponse response = listingService.confirmAvailability(userId, listingId);

        assertThat(response.lastAvailabilityConfirmedAt()).isNotNull();
        verify(listingRepository).save(listing);
    }

    @Test
    void unpublishedListingIsNotVisibleThroughPublicLookup() {
        UUID listingId = UUID.randomUUID();
        PropertyListing draft = new PropertyListing(UUID.randomUUID(), UUID.randomUUID(),
                "Private draft", "Not public", BigDecimal.valueOf(100000));
        ReflectionTestUtils.setField(draft, "id", listingId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> listingService.getPublicListing(listingId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        assertThat(draft.getStatus()).isEqualTo(ListingStatus.DRAFT);
    }

    @Test
    void publishedListingIsReturnedWithParcelResponse() {
        UUID listingId = UUID.randomUUID();
        UUID parcelId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        PropertyListing published = new PropertyListing(parcelId, UUID.randomUUID(),
                "Published parcel", "Broker-submitted details", BigDecimal.valueOf(8500000), "Broker reference");
        published.setStatus(ListingStatus.PUBLISHED, UUID.randomUUID(), "Reviewed");
        ReflectionTestUtils.setField(published, "id", listingId);
        LandParcel parcel = new LandParcel(locationId, "123", "A", BigDecimal.valueOf(2.5),
                LandMeasurementUnit.ACRE, LandType.AGRICULTURAL,
                BigDecimal.valueOf(17.24), BigDecimal.valueOf(78.43));
        ReflectionTestUtils.setField(parcel, "id", parcelId);
        Location location = new Location("Telangana", "Rangareddy", "Mandal", "Village", null);
        ReflectionTestUtils.setField(location, "id", locationId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(published));
        when(parcelRepository.findById(parcelId)).thenReturn(Optional.of(parcel));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        ListingMedia media = new ListingMedia(listingId, UUID.randomUUID() + ".png", "image/png", "Field", 0);
        ReflectionTestUtils.setField(media, "id", UUID.randomUUID());
        when(mediaRepository.findByListingIdOrderBySortOrderAscCreatedAtAsc(listingId))
                .thenReturn(java.util.List.of(media));

        ListingDtos.ListingResponse result = listingService.getPublicListing(listingId);

        assertThat(result.status()).isEqualTo(ListingStatus.PUBLISHED);
        assertThat(result.parcel().surveyNumber()).isEqualTo("123");
        assertThat(result.parcel().subdivisionNumber()).isEqualTo("A");
        assertThat(result.parcel().boundaryIsApproximate()).isTrue();
        assertThat(result.parcel().latitude()).isEqualByComparingTo("17.24");
        assertThat(result.parcel().longitude()).isEqualByComparingTo("78.43");
        assertThat(result.sourceType()).isEqualTo(ListingSourceType.BROKER_SUBMITTED);
        assertThat(result.sourceReference()).isEqualTo("Broker reference");
        assertThat(result.media()).hasSize(1);
        assertThat(result.media().getFirst().url()).contains("/api/listings/" + listingId + "/media/");
    }

    @Test
    void pendingBrokerListingCannotBePublished() {
        UUID brokerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        PropertyListing submitted = new PropertyListing(UUID.randomUUID(), brokerId,
                "Submitted", "Awaiting review", BigDecimal.valueOf(50000));
        submitted.setStatus(ListingStatus.SUBMITTED, null, null);
        ReflectionTestUtils.setField(submitted, "id", listingId);
        BrokerProfile pendingBroker = new BrokerProfile(UUID.randomUUID(), "Pending brokerage");
        ReflectionTestUtils.setField(pendingBroker, "id", brokerId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(submitted));
        when(brokerRepository.findById(brokerId)).thenReturn(Optional.of(pendingBroker));

        assertThatThrownBy(() -> listingService.changeStatus(UUID.randomUUID(), listingId,
                new AdminStatusRequest(ListingStatus.PUBLISHED, "Publish")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
        verify(listingRepository, never()).save(any(PropertyListing.class));
    }
}
