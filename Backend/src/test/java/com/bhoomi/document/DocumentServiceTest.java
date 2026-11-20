package com.bhoomi.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.common.AuditService;
import com.bhoomi.listing.PropertyListing;
import com.bhoomi.listing.PropertyListingRepository;

class DocumentServiceTest {

    private LandDocumentRepository documentRepository;
    private PropertyListingRepository listingRepository;
    private BrokerProfileRepository brokerRepository;
    private DocumentStorage storage;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        documentRepository = mock(LandDocumentRepository.class);
        listingRepository = mock(PropertyListingRepository.class);
        brokerRepository = mock(BrokerProfileRepository.class);
        storage = mock(DocumentStorage.class);
        service = new DocumentService(documentRepository, listingRepository, brokerRepository,
                storage, mock(AuditService.class));
    }

    @Test
    void brokerUploadUsesGeneratedPrivateKeyAndDetectsRealPdfContent() {
        UUID userId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        byte[] pdf = "%PDF-1.7\nprivate evidence".getBytes(StandardCharsets.US_ASCII);
        BrokerProfile broker = new BrokerProfile(userId, "Brokerage");
        ReflectionTestUtils.setField(broker, "id", brokerProfileId);
        PropertyListing listing = new PropertyListing(UUID.randomUUID(), brokerProfileId,
                "Listing", "Description", java.math.BigDecimal.valueOf(100000));
        ReflectionTestUtils.setField(listing, "id", listingId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(documentRepository.save(any(LandDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentDtos.DocumentResponse response = service.upload(userId, listingId,
                new MockMultipartFile("file", "..\\private.pdf", "text/plain", pdf), " Parcel record ");

        assertThat(response.originalFilename()).isEqualTo("private.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.provenance()).isEqualTo("BROKER_SUBMITTED");
        assertThat(response.reviewStatus()).isEqualTo(DocumentReviewStatus.PENDING);
        verify(storage).store(org.mockito.ArgumentMatchers.matches("[0-9a-f-]{36}\\.pdf"), eq(pdf));
    }

    @Test
    void brokerCannotUploadToAnotherBrokersListing() {
        UUID userId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Own brokerage");
        ReflectionTestUtils.setField(broker, "id", brokerProfileId);
        PropertyListing listing = new PropertyListing(UUID.randomUUID(), UUID.randomUUID(),
                "Other listing", "Details", java.math.BigDecimal.valueOf(100000));
        ReflectionTestUtils.setField(listing, "id", listingId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.upload(userId, listingId,
                new MockMultipartFile("file", "record.pdf", "application/pdf", "%PDF-1.7".getBytes(StandardCharsets.US_ASCII)),
                null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(storage, never()).store(anyString(), any());
    }

    @Test
    void rejectsExtensionAndContentMismatch() {
        UUID userId = UUID.randomUUID();
        UUID brokerProfileId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Brokerage");
        ReflectionTestUtils.setField(broker, "id", brokerProfileId);
        PropertyListing listing = new PropertyListing(UUID.randomUUID(), brokerProfileId,
                "Listing", "Description", java.math.BigDecimal.valueOf(100000));
        ReflectionTestUtils.setField(listing, "id", listingId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.upload(userId, listingId,
                new MockMultipartFile("file", "evidence.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.US_ASCII)),
                null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        verify(storage, never()).store(anyString(), any());
    }
}
