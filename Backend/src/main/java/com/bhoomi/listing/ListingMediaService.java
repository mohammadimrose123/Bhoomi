package com.bhoomi.listing;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.common.AuditService;
import com.bhoomi.document.DocumentStorage;

@Service
public class ListingMediaService {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 25_000_000;
    private static final int MAX_IMAGES_PER_LISTING = 10;

    private final ListingMediaRepository mediaRepository;
    private final PropertyListingRepository listingRepository;
    private final BrokerProfileRepository brokerRepository;
    private final DocumentStorage storage;
    private final AuditService auditService;

    public ListingMediaService(ListingMediaRepository mediaRepository, PropertyListingRepository listingRepository,
            BrokerProfileRepository brokerRepository, DocumentStorage storage, AuditService auditService) {
        this.mediaRepository = mediaRepository;
        this.listingRepository = listingRepository;
        this.brokerRepository = brokerRepository;
        this.storage = storage;
        this.auditService = auditService;
    }

    @Transactional
    public ListingMediaDtos.MediaResponse upload(UUID brokerUserId, UUID listingId, MultipartFile file, String caption) {
        BrokerProfile broker = brokerRepository.findByUserId(brokerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker profile required"));
        listingRepository.findById(listingId)
                .filter(candidate -> candidate.getBrokerProfileId().equals(broker.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Image must be non-empty and no larger than 10 MB");
        }
        long existingCount = mediaRepository.countByListingId(listingId);
        if (existingCount >= MAX_IMAGES_PER_LISTING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A listing can have at most 10 images");
        }
        String cleanCaption = caption == null || caption.isBlank() ? null : caption.trim();
        if (cleanCaption != null && cleanCaption.length() > 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image caption cannot exceed 300 characters");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (java.io.IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image could not be read", exception);
        }
        DetectedImage image = detectImage(file.getOriginalFilename(), content);
        String key = UUID.randomUUID() + "." + image.extension();
        storage.store(key, content);
        ListingMedia media = mediaRepository.save(new ListingMedia(listingId, key,
                image.contentType(), cleanCaption, (int) existingCount));
        auditService.record(brokerUserId, "LISTING_IMAGE_UPLOADED", "listing_media", media.getId(),
                Map.of("listingId", listingId.toString(), "mediaType", media.getMediaType()));
        return toResponse(media, listingId, false);
    }

    @Transactional(readOnly = true)
    public List<ListingMediaDtos.MediaResponse> publicMedia(UUID listingId) {
        PropertyListing listing = publishedListing(listingId);
        return mediaRepository.findByListingIdOrderBySortOrderAscCreatedAtAsc(listing.getId()).stream()
                .map(media -> toResponse(media, listingId, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public ImageContent readPublic(UUID listingId, UUID mediaId) {
        publishedListing(listingId);
        return read(mediaRepository.findByIdAndListingId(mediaId, listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing image not found")));
    }

    @Transactional(readOnly = true)
    public ImageContent readForBroker(UUID brokerUserId, UUID listingId, UUID mediaId) {
        BrokerProfile broker = brokerRepository.findByUserId(brokerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker profile required"));
        listingRepository.findById(listingId)
                .filter(candidate -> candidate.getBrokerProfileId().equals(broker.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        return read(mediaRepository.findByIdAndListingId(mediaId, listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing image not found")));
    }

    private PropertyListing publishedListing(UUID listingId) {
        return listingRepository.findById(listingId)
                .filter(candidate -> candidate.getStatus() == ListingStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
    }

    private ImageContent read(ListingMedia media) {
        return new ImageContent(media.getMediaType(), storage.read(media.getStorageKey()));
    }

    private static ListingMediaDtos.MediaResponse toResponse(ListingMedia media, UUID listingId, boolean publicUrl) {
        String route = publicUrl
                ? "/api/listings/" + listingId + "/media/" + media.getId()
                : "/api/broker/listings/" + listingId + "/media/" + media.getId();
        return new ListingMediaDtos.MediaResponse(media.getId(), route, media.getCaption(), media.getMediaType());
    }

    private static DetectedImage detectImage(String filename, byte[] content) {
        String lowerName = filename == null ? "" : filename.toLowerCase(java.util.Locale.ROOT);
        boolean pngName = lowerName.endsWith(".png");
        boolean jpegName = lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");
        if (!pngName && !jpegName) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only valid PNG and JPEG images are accepted");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image content is invalid");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image content is invalid");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                boolean png = pngName && format.equals("png");
                boolean jpeg = jpegName && (format.equals("jpeg") || format.equals("jpg"));
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((!png && !jpeg) || width < 1 || height < 1
                        || (long) width * height > MAX_IMAGE_PIXELS || reader.read(0) == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Only valid PNG and JPEG images up to 25 megapixels are accepted");
                }
                return png ? new DetectedImage("png", "image/png") : new DetectedImage("jpg", "image/jpeg");
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image content is invalid", exception);
        }
    }

    public record ImageContent(String contentType, byte[] content) {
    }

    private record DetectedImage(String extension, String contentType) {
    }
}
