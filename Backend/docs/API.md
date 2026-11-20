# Bhoomi initial API

Base URL: `http://localhost:8080`. JSON request bodies use
`Content-Type: application/json`. Protected routes require
`Authorization: Bearer <accessToken>`.

## Authentication

### `POST /api/auth/register`

Creates a buyer or a broker account. The request has no `role` or `admin`
field. Broker accounts start with `PENDING` approval and may manage profile
details and save drafts. They must be approved before submitting listings for
review.

Buyer:

```json
{
  "email": "buyer@example.com",
  "phone": "+919876543210",
  "fullName": "Example Buyer",
  "password": "a-unique-password-with-12-chars",
  "accountType": "BUYER"
}
```

Broker: use `"accountType": "BROKER"` and supply a non-empty `businessName`.
Successful registration returns `201 Created`:

```json
{
  "accessToken": "<signed-token>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "6f3a5cc8-0466-43f9-8d90-3e81b2a1b76e",
    "email": "buyer@example.com",
    "fullName": "Example Buyer",
    "role": "BUYER",
    "brokerApprovalStatus": null
  }
}
```

### `POST /api/auth/login`

```json
{"email":"buyer@example.com","password":"a-unique-password-with-12-chars"}
```

Returns the same token response as registration. Invalid credentials return
`401`; the response does not distinguish an unknown email from a wrong
password.

### `POST /api/auth/forgot-password`

Accepts `{"email":"buyer@example.com"}`. When SMTP is configured, responds
with `202 Accepted` and a generic message for both known and unknown addresses.
The response does not state whether an account exists. If delivery is not
configured, all requests receive the same `503 Service Unavailable` response;
the service does not create reset tokens or claim that an email was sent.

### `POST /api/auth/reset-password`

Accepts a reset token and a new password:

```json
{"token":"<token-from-email>","newPassword":"A-strong-example-42!"}
```

Successful requests return `204 No Content`. Tokens expire after
`PASSWORD_RESET_EXPIRATION` (15 minutes by default), can only be used once, and
are stored as SHA-256 hashes. The new password must be 12–72 characters and
include lowercase, uppercase, numeric, and special characters. Invalid,
expired, and used tokens return the same error.

Password reset email delivery is disabled by default. For local or production
SMTP, set `BHOOMI_EMAIL_ENABLED=true`, `SMTP_HOST`, `SMTP_PORT`,
`SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`, and `FRONTEND_BASE_URL` through
the environment (never commit credentials). `SMTP_STARTTLS` and `SMTP_AUTH`
default to `true`. A reset request is rate-limited to five requests per client
IP per 15 minutes; the current limiter is process-local, so deployments with
multiple instances should enforce the same or stricter limit at a shared
gateway.

Flyway migration V5 adds `password_reset_tokens` (hashed token, user reference,
expiry, used, and creation timestamps) and `users.credential_version`.
Password changes increment the credential version so previously issued
versioned access JWTs are rejected. Bhoomi has no refresh-token store.

## Health

### `GET /api/health`

Unauthenticated liveness endpoint:

```json
{"status":"UP"}
```

## Public listings

### `GET /api/listings`

Returns a page of **published only** listings. Supported query parameters:

- `page` (0-based, default 0), `size` (1–100, default 20)
- `sortBy`: `createdAt`, `askingPrice`, or `title`; `direction`: `ASC` or `DESC`
- `district`, `mandal`, `village`, `landType`
- `minPrice`, `maxPrice`
- `minExtent`, `maxExtent`; when either is supplied, `measurementUnit` is
  required (`ACRE`, `SQUARE_YARD`, `SQUARE_FOOT`, `HECTARE`, `GUNTA`)

Example: `/api/listings?district=Rangareddy&landType=AGRICULTURAL&page=0&size=10`

```json
{
  "content": [
    {
      "id": "f4e3eb3c-3a4b-4fac-a872-09299d815bb5",
      "title": "Farm parcel near the village",
      "description": "Broker-submitted listing details.",
      "askingPrice": 8500000.00,
      "currency": "INR",
      "status": "PUBLISHED",
      "sourceType": "BROKER_SUBMITTED",
      "sourceReference": null,
      "createdAt": "2026-11-02T10:00:00Z",
      "updatedAt": "2026-11-02T10:00:00Z",
      "publishedAt": "2026-11-02T11:00:00Z",
      "lastAvailabilityConfirmedAt": null,
      "parcel": {
        "id": "302962e4-b892-49f7-8bbc-8ac4fa5bd103",
        "state": "Telangana",
        "district": "Rangareddy",
        "mandal": "Example mandal",
        "village": "Example village",
        "surveyNumber": "123",
        "subdivisionNumber": "A",
        "extent": 2.5,
        "measurementUnit": "ACRE",
        "landType": "AGRICULTURAL",
        "boundaryIsApproximate": true,
        "latitude": null,
        "longitude": null
      },
      "media": []
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

`GET /api/listings/{id}` returns the same listing shape or `404` for a missing
or unpublished listing. `askingPrice` is the total broker-submitted asking
price in `currency`, not a transaction price or per-unit quote. Empty media is
a valid response; clients may show clearly labeled representative imagery.
`sourceReference` is an unverified broker-provided reference. `sourceType`
currently reports `BROKER_SUBMITTED`; it is not government verification.

The parcel may include broker-submitted WGS84 latitude/longitude coordinates.
Coordinates are optional and must be provided as a pair. Boundaries remain
separate PostGIS geometries and are marked approximate by default.

## Broker listings

Broker routes require a broker token. The backend derives the broker identity
from the JWT; clients cannot choose an owner or listing status. Pending brokers
can save drafts, but must be approved before submission.

```json
{
  "title": "Farm parcel near the village",
  "description": "Broker-submitted details; boundaries are approximate.",
  "askingPrice": 8500000,
  "parcel": {
    "state": "Telangana",
    "district": "Rangareddy",
    "mandal": "Example mandal",
    "village": "Example village",
    "surveyNumber": "123",
    "subdivisionNumber": "A",
    "extent": 2.5,
    "measurementUnit": "ACRE",
    "landType": "AGRICULTURAL",
    "latitude": 17.2400,
    "longitude": 78.4300
  },
  "sourceReference": "Optional broker-supplied reference"
}
```

- `GET /api/broker/listings` returns listings owned by the authenticated
  broker.
- `PUT /api/broker/listings/{id}` updates title, description, and asking price
- `POST /api/broker/listings/drafts` saves a draft and returns `201 Created`.
- `POST /api/broker/listings/{id}/submit` submits an owned draft or rejected
  listing for review. The broker must be approved.
- `POST /api/broker/listings` retains the direct-submission contract, but now
  requires an approved broker.
- `PATCH /api/broker/listings/{id}/status` allows the owner to mark a published
  listing `SOLD`, `UNAVAILABLE`, or `ARCHIVED`.
- `POST /api/broker/listings/{id}/availability/confirm` records an explicit
  availability confirmation for an owned published listing.
- `POST /api/broker/listings/{listingId}/media` accepts a multipart `file`
  (PNG/JPEG, max 10 MiB and 25 megapixels) and optional caption. Up to 10
  images may be attached.
- `GET /api/broker/listings/{listingId}/media/{mediaId}` reads an owned
  listing's image, including while its listing is unpublished.
- `GET /api/listings/{listingId}/media` and
  `GET /api/listings/{listingId}/media/{mediaId}` expose metadata/content only
  when the listing is published. Private storage keys are never returned.
- `GET` and `PUT /api/broker/profile` read and update the authenticated broker's
  business name/license reference and approval status.
- An edit to a rejected listing remains rejected until explicitly resubmitted.
  Other brokers receive `404` when attempting to access or modify the listing.
- Creating another active listing for the same location + survey number +
  subdivision is rejected with `409 Conflict`. Sold, unavailable, and archived
  records are not treated as active duplicates.
- Changing an asking price writes a `LISTING_ASKING_PRICE_CHANGED` audit event
  with old/new amounts and currency. Status transitions and availability
  confirmations also write audit events. Asking-price history is not verified
  transaction-price history.

## Enquiries

`POST /api/listings/{id}/enquiries` requires a buyer token and a published
listing:

```json
{"message":"Could you share more details about access to this parcel?","contactPhone":"+919876543210"}
```

Returns `201 Created`. `GET /api/broker/enquiries` requires a broker token and
returns enquiries associated with listings owned by that broker only.
`GET /api/buyer/enquiries` requires a buyer token and returns only that buyer's
enquiry history. `GET` and `PUT /api/buyer/profile` manage the authenticated
buyer's name and phone number.

## Favorites and site visits

- `GET /api/buyer/favorites` lists the authenticated buyer's saved listing IDs.
- `POST /api/buyer/favorites/{listingId}` toggles the favorite state.
- `DELETE /api/buyer/favorites/{listingId}` idempotently removes it.
- `POST /api/listings/{id}/site-visits` requires a buyer token and accepts a
  future ISO-8601 `requestedAt` timestamp and required notes.
- `GET /api/buyer/site-visits` lists only the authenticated buyer's requests.
- `GET /api/broker/site-visits` lists requests on the authenticated broker's
  listings.
- `PATCH /api/broker/site-visits/{id}/status` permits `REQUESTED` →
  `CONFIRMED`/`CANCELLED` and `CONFIRMED` → `COMPLETED`/`CANCELLED`. A
  confirmed request may be rescheduled by sending a future `requestedAt` while
  retaining status `CONFIRMED`.

All buyer routes are buyer-role restricted, broker routes are broker-role
restricted, and administration routes are admin-role restricted.

## Private land documents

Document upload and download are private broker/admin operations; no public
listing response contains document metadata, storage keys, or file contents.

- `POST /api/broker/listings/{listingId}/documents` accepts multipart form data
  with a `file` and optional `description`. Only valid PDF, PNG, and JPEG files
  up to 10 MiB are accepted. The authenticated broker must own the listing.
- `GET /api/broker/listings/{listingId}/documents` lists documents uploaded by
  that broker for the owned listing's parcel.
- `GET /api/broker/documents/{id}/download` is limited to the uploading broker.
- `GET /api/admin/documents?status=PENDING` lists review queue metadata.
- `PATCH /api/admin/documents/{id}/review` accepts `REVIEWED` or `REJECTED`
  with an optional review note.
- `GET /api/admin/documents/{id}/download` is admin-only.

Local development storage uses `BHOOMI_DOCUMENT_STORAGE_PATH` (default
`Backend/var/private-documents` when launched from `Backend`); deploy this
directory outside static web roots with restricted filesystem permissions, or
provide an alternative `DocumentStorage` implementation. Review status and
provenance describe platform handling and do not verify legal ownership or
title.

## Admin review

Admin routes require an operator-provisioned `ADMIN` token.

- `GET /api/admin/reviews` returns submitted and in-review listings.
- `PATCH /api/admin/brokers/{id}/status` accepts `APPROVED` or `REJECTED` and
  records an audit event. Broker approval is a separate admin decision from
  listing review:

```json
{"status":"APPROVED","reviewNote":"Broker credentials reviewed"}
```

- `PATCH /api/admin/listings/{id}/status` accepts an allowed next state:

```json
{"status":"UNDER_REVIEW","reviewNote":"Evidence review pending"}
```

Supported transitions are `SUBMITTED` → `UNDER_REVIEW`, `PUBLISHED`, or
`REJECTED`; `UNDER_REVIEW` → `PUBLISHED` or `REJECTED`; and `PUBLISHED` →
`SOLD`, `UNAVAILABLE`, or `ARCHIVED`. A listing can only be published after
the broker profile has separately been approved. No review status means legal
title was verified.

## Errors and data boundaries

Errors use `application/problem+json` and include a safe detail and request
path. Validation failures also include a `fieldErrors` object:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more request fields are invalid",
  "fieldErrors": {"email":"must be a well-formed email address"},
  "path": "/api/auth/register",
  "timestamp": "2026-11-02T10:00:00Z"
}
```

Land parcel geometry is stored in PostGIS with SRID 4326 and marked
approximate by default. Survey and subdivision identifiers are stored
separately and scoped by location. Asking prices are not registered transaction
prices. Land documents are private metadata with provenance; no public endpoint
returns document contents or storage keys. Broker submissions, admin review,
and independent government records are not interchangeable evidence.
