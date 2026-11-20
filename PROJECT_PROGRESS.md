# Bhoomi dynamic listing progress

Updated: 2026-11-16

## Requirement tracker

| Requirement | Status | Evidence / limitation |
|---|---|---|
| Remove mock properties from customer discovery | IMPLEMENTED | Search/detail/home inventory call backend APIs. API errors do not fall back to sample listings. Generic stock photos are explicitly labeled representative. |
| Search, filters, sorting, pagination and real counts | IMPLEMENTED (code) | Public listing API filters only `PUBLISHED`; frontend uses its page total and server pagination. Browser/database workflow is unverified. |
| Property/category/location/parcel/price data model | IMPLEMENTED (code) | Existing listing and parcel fields retained. V4 adds optional WGS84 coordinates, broker source type/reference, and last availability confirmation. Asking price remains a total INR amount. |
| Broker create/edit/submit and admin publication | IMPLEMENTED (code) | Existing broker/admin APIs and role/ownership checks retained; exact parcel duplicates among active statuses now return conflict. |
| Availability updates and freshness | IMPLEMENTED (code) | Sold/unavailable/archived statuses are excluded from published discovery; broker can record an availability confirmation. UI displays a 30-day reminder. No notification or automatic expiry job. |
| Listing images | IMPLEMENTED (code) | Uses existing `listing_media` table and private storage. Decoded PNG/JPEG, max 10 MiB/25 megapixels, at most 10 per listing. Public image access requires a published listing; broker access checks ownership. |
| Source provenance and verification claims | IMPLEMENTED (code/docs) | Listings identify as broker-submitted; optional source reference remains unverified. Existing manual document upload/review is documented and not represented as title verification. |
| Duplicate detection | PARTIALLY IMPLEMENTED | Blocks a second active listing on the exact existing parcel identity (location + survey + subdivision). Fuzzy/similar-record matching is not implemented. |
| Price/status audit | IMPLEMENTED (code) | Asking price changes, listing status changes, and availability confirmations write audit events. No transaction-price history exists. |
| External data/map providers | BLOCKED / NOT INTEGRATED | No authorized Bhu Bharati/Bhuvan service credentials or API contract in the project. OSM map link uses user-supplied coordinates; no tile integration. See `Backend/docs/DATA_PROVENANCE.md`. |
| Database migration and persisted verification | BLOCKED | V4 is forward-only and checks for pre-existing active duplicates before adding a unique partial index. PostgreSQL port 5432 is closed and Docker engine unavailable; migration, Hibernate validation, media storage and DB-backed listing flows were not run. |

## Static-data audit

- Real inventory: `LandSearch`, `ExploreLands`, and `LandDetails` fetch published
  listings from the backend. `ExploreLands` uses the API's actual total count.
- Static marketing: category cards, regional marketing cards, hero copy, and
  illustrative landscape imagery remain intentionally static; they do not
  report property availability or prices.
- Demo/sample property records: no customer discovery array remains. The
  previous empty `getDemoListings` placeholder was removed.
- Fallback content: a generic stock landscape may be used where no listing
  photo exists and is labeled “Representative image”; API failures show errors,
  never fake listings.
- Broker/admin-provided prices and location values are not independently
  verified; the UI now calls prices asking prices and surfaces source caveats.

## Validation

- Backend `mvnw.cmd clean test`: **46 passed**, 0 failures/errors/skips.
- Frontend `npm run build`: TypeScript check and Vite production build passed.
- Static source search found no demo listing arrays, hardcoded district /
  mandal / village search options, or stale “preview-only” listing claims.
- PostgreSQL/PostGIS migrations, Hibernate validation, database-backed listing
  creation/update/retrieval, and browser workflows: **BLOCKED / NOT RUN**.
  The frontend package has no automated test script.
- External provider access: Bhu Bharati URL retrieval failed in this
  environment; Bhuvan landing page was reachable but did not establish an
  authorized integration contract. OSM public tile usage policy was reviewed.

## Remaining work

1. Run V1–V4 and Hibernate validation against a disposable PostgreSQL 18 /
   PostGIS 3.6 database, without touching existing user data.
2. Verify broker create/update/image upload, admin publish, and buyer search /
   detail image display end-to-end using persisted records.
3. Add malware scanning, production object storage, retention policy, and
   upload lifecycle cleanup.
4. Add approved provider integration only after contracts, permissions, and
   credentials are supplied; otherwise continue manual evidence review.
5. Add fuzzy duplicate review, notification delivery, and availability expiry
   only with defined product rules.
6. Add landowner role only if the role and authorization model is explicitly
   expanded; current submission remains broker-only.

The listing marketplace is database-driven in code, but it is **not verified
as an operational database-backed marketplace** and must not be called
production-ready until the blocked runtime checks pass.
