# Listing data provenance and external access

## Current Bhoomi data

- Public inventory comes from `property_listings` joined to `land_parcels` and
  `locations`; only `PUBLISHED` listings are discoverable.
- The current public source type is `BROKER_SUBMITTED`. `sourceReference` is
  optional user-provided context and is not independently authenticated.
- Asking prices are broker-submitted asking prices in INR. Bhoomi does not
  claim to record completed transaction prices.
- Latitude/longitude are optional broker-submitted coordinates. PostGIS parcel
  boundaries are a separate field and remain approximate by default.
- Private supporting files may be uploaded by the listing broker and reviewed
  by an administrator. Review status and time describe the platform's review
  workflow only; they do not prove title or government-record accuracy.
- Listing state changes, availability confirmations, and asking-price changes
  are written to `audit_logs`. This provides an event history; it is not a
  validated valuation or transaction-history product.
- Exact duplicate prevention uses the existing parcel identity
  `(location, survey number, subdivision number)` and blocks a second active
  listing for that parcel. V4 also adds a partial unique index on active listings
  per parcel; if legacy data contains duplicate active listings, the migration
  stops with an explicit error and requires a reviewed manual resolution rather
  than deleting data.

## Provider access checked 2026-11-16

| Provider | Finding | Integration/access status |
|---|---|---|
| Telangana Bhu Bharati | The official portal URL `https://bhubharati.telangana.gov.in/` could not be retrieved in this environment (DNS/transport failure). No approved API contract or credentials are configured in the codebase. | Not integrated. No scraping, authentication bypass, or government verification claim. |
| ISRO Bhuvan | The public portal `https://bhuvan.nrsc.gov.in/home/index.php` was reachable and describes application sectors. The page did not establish an authorized land-record API contract for this application. | Not integrated. Service access, terms, attribution, and permission must be confirmed with the provider before use. |
| OpenStreetMap | The [OSM tile usage policy](https://operations.osmfoundation.org/policies/tiles/) requires visible attribution and imposes operational limits on its public tile server. | No OSM tile server is embedded. When coordinates exist, the UI links to an OSM map view and labels them broker-supplied. If tiles are later embedded, select an appropriate provider and comply with its attribution, caching, and usage terms. |

## Manual evidence workflow

Until provider authorization is established, brokers can upload private
supporting files for owned listings and administrators can review the file and
record a review note/status. The existing document record stores the uploader,
provenance, and review time/status. Users must independently verify legal
ownership and official records. No current backend endpoint records an
authorized government/provider response; a document review is not an
equivalent.

## Known gaps

- No owner/landowner account role exists; listing submission is broker-only.
- Similar-but-not-identical parcel duplicate matching is not implemented.
- Availability reminders are shown in the broker studio after 30 days or when
  no confirmation is recorded; no email/SMS notification or automatic expiry
  job is configured.
- Location map links are available only when coordinates were supplied.
- Database migration, media filesystem behavior, and persisted end-to-end flows
  still require PostgreSQL/PostGIS runtime verification.
