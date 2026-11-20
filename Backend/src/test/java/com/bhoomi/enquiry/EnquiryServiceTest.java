package com.bhoomi.enquiry;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.listing.ListingStatus;
import com.bhoomi.listing.PropertyListing;
import com.bhoomi.listing.PropertyListingRepository;

class EnquiryServiceTest {

    @Test
    void buyerCanReadOnlyTheirOwnEnquiryHistory() {
        EnquiryRepository enquiryRepository = mock(EnquiryRepository.class);
        UUID buyerId = UUID.randomUUID();
        Enquiry enquiry = new Enquiry(UUID.randomUUID(), buyerId, "Please share more details.", null);
        when(enquiryRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)).thenReturn(List.of(enquiry));
        EnquiryService service = new EnquiryService(enquiryRepository, mock(PropertyListingRepository.class),
                mock(BrokerProfileRepository.class));

        List<EnquiryDtos.EnquiryResponse> response = service.forBuyer(buyerId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).buyerId()).isEqualTo(buyerId);
        org.mockito.Mockito.verify(enquiryRepository).findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    @Test
    void buyerCanEnquireAboutPublishedListing() {
        EnquiryRepository enquiryRepository = mock(EnquiryRepository.class);
        PropertyListingRepository listingRepository = mock(PropertyListingRepository.class);
        EnquiryService service = new EnquiryService(enquiryRepository, listingRepository,
                mock(BrokerProfileRepository.class));
        UUID buyerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        PropertyListing published = new PropertyListing(UUID.randomUUID(), UUID.randomUUID(),
                "Published", "Details", java.math.BigDecimal.valueOf(1000));
        ReflectionTestUtils.setField(published, "id", listingId);
        published.setStatus(ListingStatus.PUBLISHED, UUID.randomUUID(), "Reviewed");
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(published));
        when(enquiryRepository.save(org.mockito.ArgumentMatchers.any(Enquiry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnquiryDtos.EnquiryResponse response = service.create(buyerId, listingId,
                new EnquiryDtos.CreateRequest("I would like details about this parcel.", "+919876543210"));

        assertThat(response.listingId()).isEqualTo(listingId);
        assertThat(response.buyerId()).isEqualTo(buyerId);
        org.mockito.ArgumentCaptor<Enquiry> captor = org.mockito.ArgumentCaptor.forClass(Enquiry.class);
        org.mockito.Mockito.verify(enquiryRepository).save(captor.capture());
        assertThat(captor.getValue().getBuyerId()).isEqualTo(buyerId);
        assertThat(captor.getValue().getStatus()).isEqualTo(EnquiryStatus.OPEN);
    }

    @Test
    void enquiryCannotBeCreatedForUnpublishedListing() {
        EnquiryRepository enquiryRepository = mock(EnquiryRepository.class);
        PropertyListingRepository listingRepository = mock(PropertyListingRepository.class);
        EnquiryService service = new EnquiryService(enquiryRepository, listingRepository,
                mock(BrokerProfileRepository.class));
        UUID listingId = UUID.randomUUID();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(new PropertyListing(
                UUID.randomUUID(), UUID.randomUUID(), "Draft", "Not published",
                java.math.BigDecimal.valueOf(1000))));

        assertThatThrownBy(() -> service.create(UUID.randomUUID(), listingId,
                new EnquiryDtos.CreateRequest("I would like to know more about this land.", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        org.mockito.Mockito.verify(enquiryRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any(Enquiry.class));
    }
}
