package com.bhoomi.document;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.bhoomi.broker.BrokerProfile;
import com.bhoomi.broker.BrokerProfileRepository;
import com.bhoomi.common.AuditService;
import com.bhoomi.listing.PropertyListing;
import com.bhoomi.listing.PropertyListingRepository;

@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private final LandDocumentRepository documentRepository;
    private final PropertyListingRepository listingRepository;
    private final BrokerProfileRepository brokerRepository;
    private final DocumentStorage storage;
    private final AuditService auditService;

    public DocumentService(LandDocumentRepository documentRepository,
            PropertyListingRepository listingRepository, BrokerProfileRepository brokerRepository,
            DocumentStorage storage, AuditService auditService) {
        this.documentRepository = documentRepository;
        this.listingRepository = listingRepository;
        this.brokerRepository = brokerRepository;
        this.storage = storage;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentDtos.DocumentResponse upload(UUID brokerUserId, UUID listingId, MultipartFile file,
            String description) {
        BrokerProfile broker = brokerFor(brokerUserId);
        PropertyListing listing = listingRepository.findById(listingId)
                .filter(candidate -> candidate.getBrokerProfileId().equals(broker.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Document must be non-empty and no larger than 10 MB");
        }
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        byte[] content;
        try {
            content = file.getBytes();
        } catch (java.io.IOException exception) {
            throw new DocumentStorageException("Uploaded document could not be read", exception);
        }
        DetectedFile detected = detectFile(originalFilename, content);
        String cleanDescription = description == null ? null : description.trim();
        if (cleanDescription != null && cleanDescription.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document description cannot exceed 500 characters");
        }
        String storageKey = UUID.randomUUID() + "." + detected.extension();
        storage.store(storageKey, content);
        LandDocument document = documentRepository.save(new LandDocument(listing.getLandParcelId(), brokerUserId,
                storageKey, originalFilename, detected.contentType(), "BROKER_SUBMITTED", cleanDescription));
        auditService.record(brokerUserId, "LAND_DOCUMENT_UPLOADED", "land_documents", document.getId(),
                Map.of("reviewStatus", document.getReviewStatus().name()));
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentResponse> forBrokerListing(UUID brokerUserId, UUID listingId) {
        BrokerProfile broker = brokerFor(brokerUserId);
        PropertyListing listing = listingRepository.findById(listingId)
                .filter(candidate -> candidate.getBrokerProfileId().equals(broker.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found"));
        return documentRepository.findByLandParcelIdAndUploadedByOrderByCreatedAtDesc(
                        listing.getLandParcelId(), brokerUserId).stream()
                .map(DocumentService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentResponse> adminQueue(DocumentReviewStatus status) {
        return documentRepository.findByReviewStatusOrderByCreatedAtAsc(status).stream()
                .map(DocumentService::toResponse)
                .toList();
    }

    @Transactional
    public DocumentDtos.DocumentResponse review(UUID adminId, UUID documentId, DocumentDtos.ReviewRequest request) {
        if (request.status() == DocumentReviewStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Document review must be REVIEWED or REJECTED");
        }
        LandDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        document.review(request.status(), adminId, request.reviewNote());
        document = documentRepository.save(document);
        auditService.record(adminId, "LAND_DOCUMENT_REVIEWED", "land_documents", document.getId(),
                Map.of("reviewStatus", document.getReviewStatus().name()));
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public Download downloadForBroker(UUID brokerUserId, UUID documentId) {
        LandDocument document = documentRepository.findByIdAndUploadedBy(documentId, brokerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return download(document);
    }

    @Transactional(readOnly = true)
    public Download downloadForAdmin(UUID documentId) {
        LandDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return download(document);
    }

    private Download download(LandDocument document) {
        return new Download(toResponse(document), document.getContentType(), storage.read(document.getStorageKey()));
    }

    private BrokerProfile brokerFor(UUID userId) {
        return brokerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Broker profile required"));
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A document filename is required");
        }
        String basename = filename.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "").trim();
        if (basename.isBlank() || basename.length() > 255 || basename.equals(".") || basename.equals("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The document filename is invalid");
        }
        return basename;
    }

    private static DetectedFile detectFile(String filename, byte[] content) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
        if (extension.equals("jpeg")) extension = "jpg";
        boolean pdf = extension.equals("pdf") && content.length >= 5
                && new String(content, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-");
        boolean png = extension.equals("png") && content.length >= 8
                && content[0] == (byte) 0x89 && content[1] == 'P' && content[2] == 'N'
                && content[3] == 'G' && content[4] == 0x0d && content[5] == 0x0a
                && content[6] == 0x1a && content[7] == 0x0a;
        boolean jpeg = extension.equals("jpg") && content.length >= 3
                && content[0] == (byte) 0xff && content[1] == (byte) 0xd8 && content[2] == (byte) 0xff;
        if (pdf) return new DetectedFile("pdf", "application/pdf");
        if (png) return new DetectedFile("png", "image/png");
        if (jpeg) return new DetectedFile("jpg", "image/jpeg");
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Only valid PDF, PNG, and JPEG documents are accepted");
    }

    private static DocumentDtos.DocumentResponse toResponse(LandDocument document) {
        return new DocumentDtos.DocumentResponse(document.getId(), document.getLandParcelId(),
                document.getOriginalFilename(), document.getContentType(), document.getProvenance(),
                document.getDescription(), document.getReviewStatus(), document.getReviewedAt(),
                document.getReviewNote(), document.getCreatedAt());
    }

    public record Download(DocumentDtos.DocumentResponse metadata, String contentType, byte[] content) {
    }

    private record DetectedFile(String extension, String contentType) {
    }
}
