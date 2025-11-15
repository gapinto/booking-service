# RFC: Booking Service

Status: Proposed
Owners: Guilherme Andrade
Last Updated: 2025-11-14

## Context
Build a REST service to manage bookings and blocks for properties, enforcing overlap validation and core CRUD flows. Use an in-memory volatile DB (H2).

## Problem Statement
- Create/update/cancel/rebook/delete and get bookings.
- Create/update/delete blocks.
- Ensure integrity: no date overlaps between active bookings and blocks for the same property.

## Goals
- Clear, validated REST API.
- Centralized overlap rule with tests.
- Runnable locally via Docker.

## Non-Goals
- Authentication/authorization.
+- Advanced multi-property features (pricing, payments).
+- Soft delete/auditing.

## Architecture Overview
- Spring Boot 3, Java 17, in-memory H2.
- Entities: `Booking`, `Block`.
- Overlap rule enforced in services (single point).

## Data Model
- Booking: id (UUID), propertyId (string), guestName, guestEmail, startDate, endDate, status (ACTIVE/CANCELED).
- Block: id (UUID), propertyId, startDate, endDate.

## Invariants and Validation
- `endDate >= startDate`.
- ACTIVE booking cannot overlap:
  - other non-canceled bookings;
  - blocks.
- Block cannot overlap active bookings (and we also forbid block-vs-block overlaps).

## API
Base path: `/api`

### Bookings
- POST `/bookings`
  - Body: `{ propertyId, guestName, guestEmail, startDate, endDate }`
  - 201 Created + body; 409 on conflict; 400 on validation error.
- GET `/bookings/{id}`
  - 200 OK + body; 404 if not found.
- PUT `/bookings/{id}`
  - Body: `{ guestName, guestEmail, startDate, endDate }`
  - 200 OK; 409 on conflict; 400 invalid; 404 not found.
- POST `/bookings/{id}/cancel`
  - 200 OK; 404 if not found.
- POST `/bookings/{id}/rebook`
  - 200 OK; 409 on conflict; 404 not found.
- DELETE `/bookings/{id}`
  - 204 No Content (idempotent).

### Blocks
- POST `/blocks`
  - Body: `{ propertyId, startDate, endDate }`
  - 201 Created; 409 if it conflicts with an active booking or another block.
- PUT `/blocks/{id}`
  - Body: `{ propertyId, startDate, endDate }`
  - 200 OK; 409 on conflict; 404 not found.
- DELETE `/blocks/{id}`
  - 204 No Content.

## Error Handling
- 400 BAD_REQUEST: validation (required fields, date range).
- 404 NOT_FOUND: resource not found.
- 409 CONFLICT: overlapping periods.
Payload: `{ "error": "CONFLICT|BAD_REQUEST|NOT_FOUND", "message": "...", "timestamp": "..." }`

## Trade-offs
- Keep separate entities (Booking/Block) and extract rule to a service, avoiding JPA inheritance.
- Do not introduce a persisted “Allocation”; simpler for this scope.

## Alternatives Considered
- Single “allocations” table with `type`. (+) unified reporting, (–) unnecessary complexity here.
- Allow block overlaps and merge periods; rejected for clarity.

## Testing Strategy
- TDD on booking service (creation and conflicts).
- API tests with MockMvc for main flows and error mapping.

## Deployment / Operations
- Multi-stage Dockerfile; `docker-compose up --build` exposes `:8080`.
- In-memory DB (volatile).

## Open Questions
- Role-based authorization (owner/manager vs guest).
- Update policy on canceled bookings (recreate vs rebook).


