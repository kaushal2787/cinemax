# CineMax Platform — Solution Architecture & Design Document

## 1. Executive Summary

CineMax is a **dual-sided marketplace** for movie ticket booking, serving:
- **B2B**: Theatre partners onboarding their venues, managing shows, and tracking revenue.
- **B2C**: End customers browsing movies by city/language/genre and booking tickets seamlessly.

**Tech Stack**: Java 17 · Spring Boot 3.2 · PostgreSQL · Redis · AWS · Claude AI

---

## 2. Functional Features

### 2.1 B2C — Customer Features
| Feature | Implementation |
|---|---|
| Browse movies by city, language, genre | REST API with indexed DB queries + Redis cache |
| View shows by date with timings | Composite index on (movie_id, show_time, status) |
| Seat layout with availability | Redis-cached ShowSeat table |
| Book tickets with seat selection | Optimistic locking (version field) |
| Offers: 50% on 3rd ticket | OfferService.calculatePrice() |
| Offers: 20% on afternoon shows | ShowPeriod enum (12PM–5PM = AFTERNOON) |
| Bulk booking (up to 50 tickets) | BulkBookingRequest DTO |
| Booking cancellation with refund | Async refund via payment gateway |
| Price preview before payment | /api/v1/shows/price endpoint |

### 2.2 B2B — Theatre Partner Features
| Feature | Implementation |
|---|---|
| Theatre onboarding | TheatreOnboardRequest with screen/seat layout |
| Create/Update/Delete shows | ShowManagementController + ShowManagementService |
| Seat inventory allocation | ShowSeat records created per show |
| Revenue dashboard | Analytics queries on bookings table |
| Partner approval workflow | Admin approval → status: ACTIVE |

---

## 3. API Contract

### Base URL: `https://api.cinemax.com/v1`

### Authentication
```
POST /auth/register       — Customer/Partner registration
POST /auth/login          — Returns JWT + refresh token
POST /auth/refresh        — Token rotation
```

### Shows (Public)
```
GET  /shows?city=&movieId=&date=&language=&screenType=
                          — Browse shows grouped by theatre
GET  /shows/{id}/seats    — Seat layout with availability
POST /shows/price         — Price calculation with offers
```

### Bookings (Customer)
```
POST   /bookings          — Book tickets
POST   /bookings/bulk     — Bulk booking (≤50)
GET    /bookings/{ref}    — Get booking details
DELETE /bookings/{ref}    — Cancel booking
POST   /bookings/cancel/bulk — Bulk cancellation
```

### Theatre Partner (B2B)
```
POST /partner/theatres          — Onboard theatre
GET  /partner/theatres          — List my theatres
POST /partner/shows             — Create show
PUT  /partner/shows/{id}        — Update show
DELETE /partner/shows/{id}      — Cancel show
POST /partner/shows/{id}/inventory — Update seat inventory
```

---

## 4. Database & Data Model

### Key Design Decisions
1. **ShowSeat table**: Pre-creates one row per seat per show for O(1) seat status lookup.
2. **Optimistic Locking** (`version` column on `show_seats`): Prevents double-booking without exclusive locks.
3. **Composite indexes**: `(movie_id, show_time, status)` enables the main browse query to hit index-only scans.
4. **JSONB for offers**: `applicable_cities` and `applicable_theatre_ids` stored as JSONB for flexible querying.
5. **Flyway migrations**: Schema versioned, repeatable, auditable.

### Tables Summary
```
theatre_partners → theatres → screens → seats
                                      → show_seats ← shows ← movies
users → bookings → booking_items → show_seats
offers (platform-wide discount rules)
```

---

## 5. Design Patterns Used

| Pattern | Where Used | Why |
|---|---|---|
| Repository Pattern | All data access via Spring Data JPA | Decouples business logic from DB |
| Strategy Pattern | OfferService discount calculation | Pluggable discount strategies |
| Builder Pattern | All DTOs and Entities (Lombok @Builder) | Readable object construction |
| Optimistic Locking | ShowSeat.version | High-concurrency seat booking |
| Cache-Aside | Redis cache on show listings | Reduces DB load for read-heavy traffic |
| Circuit Breaker | Resilience4j on PaymentService | Graceful payment gateway failures |
| Observer/Event | Domain events for async processing | Decoupled notifications & refunds |
| Factory Method | BookingReferenceGenerator | Consistent reference format |
| DTO Pattern | Request/Response DTOs | API contract isolation |
| Saga Pattern | Booking → Payment → Confirm flow | Distributed transaction management |

---

## 6. Non-Functional Requirements

### 6.1 Transactional Scenarios

**Concurrent Seat Booking (most critical)**
- Problem: Two users book the same seat simultaneously.
- Solution: `@Version` on `ShowSeat` entity triggers `OptimisticLockException` on concurrent writes.
- Fallback: User gets "seat no longer available" error and is prompted to re-select.
- Lock duration: Seat is LOCKED for 10 minutes during payment; auto-released via scheduler.

**Payment Failure Handling**
- Booking stays in PENDING state for 10 minutes.
- Scheduled job (`@Scheduled`) releases LOCKED seats if payment not completed.
- Idempotent payment initiation (Razorpay order ID = booking reference).
- Webhook confirmation updates booking to CONFIRMED.

**Show Cancellation by Partner**
- Transactional: Mark show CANCELLED + trigger async refund job for all confirmed bookings.
- Notification: Email/SMS to all affected customers via SNS.

### 6.2 Scaling to Multiple Cities/Countries

**Multi-Region Architecture (AWS)**
```
Primary Region (Mumbai):     ap-south-1
Failover Region (Singapore): ap-southeast-1
DR Region (Frankfurt):       eu-central-1
```

- **Route 53 Latency Routing**: Routes users to nearest healthy region.
- **Aurora Global Database**: Primary writer in ap-south-1, read replicas in each region.
- **Redis Cluster (ElastiCache)**: Regional clusters; show listings cached per region.
- **S3 + CloudFront**: Movie posters/assets served from edge locations.
- **SQS FIFO queues**: Booking events processed in order, per-show message group.

**99.99% Availability Calculation**
- Multi-AZ deployment within each region (RDS Multi-AZ, ECS across 3 AZs)
- Aurora with automated failover (< 30s RTO)
- Route 53 health checks with DNS failover
- ALB with cross-AZ load balancing
- Target: 52 minutes downtime/year

### 6.3 Theatre Integration Strategy

**New Digital Theatres**: Native API onboarding via the partner portal.

**Theatres with Existing IT Systems**:
- **Integration Adapter Pattern**: Per-theatre adapter implementing `TheatreIntegrationPort` interface.
- **Standard formats**: REST or SOAP adapters, BookMyShow XML, custom CSV.
- **Webhook events**: CineMax pushes booking events to partner webhooks.
- **Scheduled sync**: Nightly reconciliation of show schedules and seat inventory.

**Localization**:
- `movies.language` field supports all Indian languages + international.
- `Accept-Language` header respected for UI strings (i18n via Spring MessageSource).
- Currency: INR default; config-based for international expansion.
- Timezone: Show times stored in UTC, displayed in theatre's local timezone.

### 6.4 Payment Gateway Integration

**Primary**: Razorpay (India)
**International**: Stripe
**Fallback**: PayU

```
1. POST /bookings       → Creates order in Razorpay
2. Client redirected to payment page
3. Razorpay webhook → POST /webhooks/payment
4. Idempotency check (booking_reference)
5. Confirm booking + release seat lock
6. Send confirmation email/SMS via SNS → SES/SNS
```

Circuit breaker wraps all payment calls (Resilience4j):
- Opens after 5 failures in 10s window
- Half-open probe every 30s
- Fallback: Queue booking for async payment retry

### 6.5 Monetization

| Revenue Stream | Implementation |
|---|---|
| Convenience fee (2% per booking) | Applied in PriceCalculationResponse |
| Theatre onboarding fee | Partner subscription plans |
| Revenue share per ticket (varies by tier) | Configurable rate per TheatrePartner |
| Premium listing for movies | Movie.featured flag |
| Advertising inventory | Ad slots in movie listings |
| Data analytics dashboard for partners | Paid B2B analytics feature |
| Cancellation/refund fee (after threshold) | Configurable cancellation policy |

### 6.6 OWASP Top 10 Protection

| # | Threat | Mitigation |
|---|---|---|
| A01 | Broken Access Control | @PreAuthorize on all endpoints; partner can only manage own theatres |
| A02 | Cryptographic Failures | BCrypt (cost 12) for passwords; JWT HS256; HTTPS enforced |
| A03 | Injection | JPA parameterised queries; no raw SQL; Hibernate |
| A04 | Insecure Design | Optimistic locking; seat lock TTL; payment idempotency |
| A05 | Security Misconfiguration | Security headers (CSP, X-Frame-Options, HSTS); strict CORS |
| A06 | Vulnerable Components | OWASP Dependency Check Maven plugin; automated CVE scanning |
| A07 | Identification/Auth Failures | Rate limiting (5 login attempts/min per IP); refresh token rotation |
| A08 | Software & Data Integrity | Docker image signing; artifact checksums in CI |
| A09 | Security Logging & Monitoring | Structured JSON logs → CloudWatch; alert on auth failures |
| A10 | SSRF | No user-controlled URLs fetched by backend |

### 6.7 Compliance

| Requirement | Implementation |
|---|---|
| PCI DSS | No card data stored; Razorpay handles tokenisation |
| GDPR/PDPB | User consent on registration; right to erasure API |
| GST | 18% GST applied on (base + convenience fee); tax invoice generated |
| Ticket Act (India) | Max booking of 10 tickets per transaction per person |

---

## 7. Platform Provisioning & Sizing

### 7.1 Technology Choices

| Decision | Choice | Rationale |
|---|---|---|
| Language | Java 17 | Virtual threads (Project Loom), record types, modern GC |
| Framework | Spring Boot 3.2 | Mature, native AWS SDK, excellent JPA/Security/Cache |
| Database | PostgreSQL (Aurora) | ACID, JSONB, pg_trgm for search, multi-region |
| Cache | Redis (ElastiCache) | Sub-ms reads for show listings, seat lock TTL |
| Queue | SQS FIFO | Ordered booking events, exactly-once delivery |
| Container | ECS Fargate | Serverless containers, no EC2 management |
| CDN | CloudFront | Low-latency asset delivery, DDoS protection |
| Search | OpenSearch | Full-text movie/theatre search with filters |
| Monitoring | CloudWatch + Grafana | Metrics, logs, dashboards |
| AI | Claude (Anthropic) | Smart recommendations, natural language search |

### 7.2 Sizing Estimates (Peak Load)

**Assumptions**:
- 10 cities, 500 theatres, 5,000 screens
- Peak: 50,000 concurrent users (Friday evening premiere)
- 200 bookings/second during rush

| Component | Configuration | Justification |
|---|---|---|
| ECS (API) | 4–20 tasks × 2vCPU 4GB | Auto-scaling on CPU > 60% |
| Aurora PostgreSQL | db.r6g.2xlarge (8 vCPU, 64GB) | 5,000 IOPS sustained |
| ElastiCache Redis | cache.r6g.xlarge × 2 (cluster) | 26GB RAM, 6.5Gbps |
| SQS | Standard queues | Unlimited throughput |
| CloudFront | Edge locations | Unlimited |
| RDS Proxy | 2 proxies | Connection pooling for Lambda |

### 7.3 COTS Enterprise Systems

| System | Purpose |
|---|---|
| Razorpay/Stripe | Payment processing |
| SendGrid/SES | Transactional email |
| Twilio/SNS | SMS notifications |
| Firebase | Push notifications (mobile) |
| Datadog | APM + infrastructure monitoring |
| PagerDuty | On-call alerting |
| Okta | SSO for admin/partner portal |
| Jira | Issue tracking |
| Confluence | Documentation |

### 7.4 Release Management

**CI/CD Pipeline (GitHub Actions → AWS CodePipeline)**:
```
Push → Build (Maven) → Unit Tests → Integration Tests →
Docker Build → ECR Push → Staging Deploy → Smoke Tests →
Production Deploy (Blue/Green) → Monitoring Alerts
```

**Blue/Green Deployment**:
- Two identical ECS service groups (Blue = current, Green = new)
- ALB traffic shifted 10% → 50% → 100% over 15 minutes
- Automatic rollback if error rate > 1% in CloudWatch

**Internationalization Release Strategy**:
- Feature flags (AWS AppConfig) control rollout per city/region
- i18n bundles in S3, loaded dynamically
- Database migrations run before code deploy (Flyway)
- Zero-downtime schema changes (additive only, backward-compatible)

### 7.5 Monitoring & Observability

**Three Pillars**:
1. **Metrics** (CloudWatch/Grafana): API latency p50/p95/p99, booking success rate, payment conversion, seat lock TTL violations
2. **Logs** (CloudWatch Logs → OpenSearch): Structured JSON, correlation ID per request, booking lifecycle events
3. **Traces** (AWS X-Ray): End-to-end latency from API → DB → Redis → Payment

**Key Alerts**:
- Booking failure rate > 2% → PagerDuty P1
- Payment gateway circuit breaker opens → P1
- DB connection pool exhausted → P1
- Show sell-out rate spike → Dashboard

**KPIs**:

| KPI | Target |
|---|---|
| API P95 latency | < 200ms |
| Platform availability | 99.99% |
| Booking success rate | > 98% |
| Payment conversion | > 92% |
| Seat lock timeout rate | < 1% |
| Time to onboard new theatre | < 2 hours |
| Customer NPS | > 50 |

---

## 8. Project Plan & Estimates

### Phase 1 – MVP (12 weeks)
| Stream | Weeks | Effort |
|---|---|---|
| Core API (Shows, Bookings, Auth) | 1–6 | 3 engineers |
| B2B Partner Portal | 3–8 | 2 engineers |
| Payment Integration | 5–8 | 1 engineer |
| Infrastructure (AWS, CI/CD) | 1–4 | 1 DevOps |
| QA & Testing | 7–12 | 2 QA engineers |
| **Total MVP** | **12 weeks** | **~9 FTEs** |

### Phase 2 – Scale (8 weeks)
| Stream | Scope |
|---|---|
| Multi-city/language support | 3 weeks |
| AI recommendations (Claude) | 2 weeks |
| Analytics dashboard | 2 weeks |
| International payments | 1 week |

### Phase 3 – Growth (ongoing)
- Mobile apps (iOS/Android)
- Corporate/bulk booking portal
- Loyalty program
- Dynamic pricing

---

## 9. Stakeholder Management

### Key Stakeholders
| Stakeholder | Interest | Engagement Strategy |
|---|---|---|
| Theatre Partners (B2B) | Revenue, ease of use, support | Monthly partner reviews, SLA commitments, dedicated partner success manager |
| End Customers (B2C) | Seamless booking, fair pricing | NPS surveys, in-app feedback, community forums |
| Payment Partner (Razorpay) | Integration stability, compliance | Quarterly technical syncs, dedicated integration support |
| Legal/Compliance | PDPB, GST, Ticket Act | Monthly compliance reviews, automated audit trails |
| Engineering Leadership | Delivery timelines, tech debt | Weekly sprint reviews, architecture decision records (ADRs) |

### Decision Instances
| Scenario | Action Taken |
|---|---|
| Theatre partner requests custom pricing model | Analysed impact, built configurable commission rates, shipped in Phase 1.5 |
| Payment gateway downtime during peak | Pre-agreed fallback gateway (PayU), circuit breaker auto-switches |
| PDPB compliance requirement | Right-to-erasure API added in Phase 2, DPA agreements signed with all processors |
| City expansion request | Feature flag-based rollout, no code change needed |

---

## 10. Claude AI Integration

**Use Cases**:
1. **Natural language search**: "Show me romantic movies this weekend near Indiranagar" → parsed by Claude → structured query
2. **Smart recommendations**: Based on booking history, Claude suggests movies/shows
3. **Chatbot support**: Partner onboarding assistant, customer support
4. **Content moderation**: Movie descriptions, reviews
5. **Dynamic offer generation**: Claude analyses booking patterns → suggests targeted offers

**Implementation**:
```java
// AI-powered movie search
String userQuery = "romantic movies in Hindi tonight near me";
ClaudeResponse parsed = claudeService.parseSearchIntent(userQuery);
// Returns: {genre: "Romance", language: "Hindi", date: "today", location: "user's city"}
ShowSearchRequest request = mapper.map(parsed, ShowSearchRequest.class);
return showService.browseShowsByMovieAndCity(request);
```
