package com.bhoomi.listing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.common.AuditService;
import com.bhoomi.document.DocumentStorage;

class ListingMediaServiceTest {

    private ListingMediaRepository mediaRepository;
    private PropertyListingRepository listingRepository;
    private BrokerProfileRepository brokerRepository;
    private DocumentStorage storage;
    private ListingMediaService service;

    @BeforeEach
    void setUp() {
        mediaRepository = mock(ListingMediaRepository.class);
        listingRepository = mock(PropertyListingRepository.class);
        brokerRepository = mock(BrokerProfileRepository.class);
        storage = mock(DocumentStorage.class);
        service = new ListingMediaService(mediaRepository, listingRepository, brokerRepository,
                storage, mock(AuditService.class));
    }

    @Test
    void brokerCanUploadSignatureCheckedImageForOwnedListing() {
        UUID userId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Broker");
        ReflectionTestUtils.setField(broker, "id", brokerId);
        PropertyListing listing = new PropertyListing(UUID.randomUUID(), brokerId,
                "Listing", "Description", java.math.BigDecimal.valueOf(500000));
        ReflectionTestUtils.setField(listing, "id", listingId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(mediaRepository.save(any(ListingMedia.class))).thenAnswer(invocation -> {
            ListingMedia media = invocation.getArgument(0);
            ReflectionTestUtils.setField(media, "id", UUID.randomUUID());
            return media;
        });
        byte[] pngSignature = createPng();

        ListingMediaDtos.MediaResponse response = service.upload(userId, listingId,
                new MockMultipartFile("file", "parcel.png", "text/plain", pngSignature), "Frontage");

        assertThat(response.mediaType()).isEqualTo("image/png");
        assertThat(response.url()).contains("/api/broker/listings/" + listingId + "/media/");
        verify(storage).store(org.mockito.ArgumentMatchers.matches("[0-9a-f-]{36}\\.png"), any(byte[].class));
    }

    @Test
    void brokerCannotUploadImageToAnotherBrokersListing() {
        UUID userId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Broker");
        ReflectionTestUtils.setField(broker, "id", brokerId);
        PropertyListing listing = new PropertyListing(UUID.randomUUID(), UUID.randomUUID(),
                "Other listing", "Description", java.math.BigDecimal.valueOf(500000));
        ReflectionTestUtils.setField(listing, "id", listingId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.upload(userId, listingId,
                new MockMultipartFile("file", "parcel.png", "image/png",
                        createPng()), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(storage, never()).store(any(), any());
    }

    @Test
    void unpublishedListingMediaIsNotPubliclyReadable() {
        UUID listingId = UUID.randomUUID();
        PropertyListing draft = new PropertyListing(UUID.randomUUID(), UUID.randomUUID(),
                "Draft", "Private", java.math.BigDecimal.valueOf(100000));
        ReflectionTestUtils.setField(draft, "id", listingId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.publicMedia(listingId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(mediaRepository, never()).findByListingIdOrderBySortOrderAscCreatedAtAsc(listingId);
    }

    @Test
    void publishedListingImageCanBeReadUsingItsListingScopedId() {
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        PropertyListing listing = new PropertyListing(UUID.randomUUID(), UUID.randomUUID(),
                "Published", "Details", java.math.BigDecimal.valueOf(500000));
        listing.setStatus(ListingStatus.PUBLISHED, UUID.randomUUID(), null);
        ReflectionTestUtils.setField(listing, "id", listingId);
        ListingMedia media = new ListingMedia(listingId, UUID.randomUUID() + ".png", "image/png", null, 0);
        ReflectionTestUtils.setField(media, "id", mediaId);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(mediaRepository.findByIdAndListingId(mediaId, listingId)).thenReturn(Optional.of(media));
        when(storage.read(media.getStorageKey())).thenReturn(new byte[] {1, 2, 3});

        ListingMediaService.ImageContent response = service.readPublic(listingId, mediaId);

        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.content()).containsExactly(1, 2, 3);
    }

    @Test
    void brokerCannotReadAnotherBrokersUnpublishedImage() {
        UUID userId = UUID.randomUUID();
        UUID brokerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID mediaId = UUID.randomUUID();
        BrokerProfile broker = new BrokerProfile(userId, "Broker");
        ReflectionTestUtils.setField(broker, "id", brokerId);
        PropertyListing listing = new PropertyListing(UUID.randomUUID(), UUID.randomUUID(),
                "Other draft", "Private", java.math.BigDecimal.valueOf(500000));
        ReflectionTestUtils.setField(listing, "id", listingId);
        when(brokerRepository.findByUserId(userId)).thenReturn(Optional.of(broker));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThatThrownBy(() -> service.readForBroker(userId, listingId, mediaId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
        verify(mediaRepository, never()).findByIdAndListingId(mediaId, listingId);
        verify(storage, never()).read(org.mockito.ArgumentMatchers.anyString());
    }

    private static byte[] createPng() {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Test PNG could not be generated", exception);
        }
    }
}
