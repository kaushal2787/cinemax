-- V1__init_schema.sql
-- CineMax Platform - Initial Database Schema

-- ─────────────────────────────────────────────
-- Extensions
-- ─────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- for full-text search on movie titles

-- ─────────────────────────────────────────────
-- Theatre Partners (B2B)
-- ─────────────────────────────────────────────
CREATE TABLE theatre_partners (
    id              BIGSERIAL PRIMARY KEY,
    business_name   VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(20),
    gst_number      VARCHAR(20),
    pan_number      VARCHAR(20),
    bank_account_no VARCHAR(50),
    ifsc_code       VARCHAR(20),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────
-- Users
-- ─────────────────────────────────────────────
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    phone         VARCHAR(20),
    city          VARCHAR(100),
    role          VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    is_verified   BOOLEAN DEFAULT FALSE,
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);

-- ─────────────────────────────────────────────
-- Theatres
-- ─────────────────────────────────────────────
CREATE TABLE theatres (
    id              BIGSERIAL PRIMARY KEY,
    partner_id      BIGINT REFERENCES theatre_partners(id),
    name            VARCHAR(255) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100),
    country         VARCHAR(100) DEFAULT 'India',
    address         TEXT,
    pincode         VARCHAR(10),
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(20),
    rating          DECIMAL(3,2) DEFAULT 0.00,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_theatres_city      ON theatres(city);
CREATE INDEX idx_theatres_partner   ON theatres(partner_id);
CREATE INDEX idx_theatres_status    ON theatres(status);

-- ─────────────────────────────────────────────
-- Screens
-- ─────────────────────────────────────────────
CREATE TABLE screens (
    id           BIGSERIAL PRIMARY KEY,
    theatre_id   BIGINT NOT NULL REFERENCES theatres(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    total_seats  INTEGER NOT NULL,
    screen_type  VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    UNIQUE (theatre_id, name)
);
CREATE INDEX idx_screens_theatre ON screens(theatre_id);

-- ─────────────────────────────────────────────
-- Seats
-- ─────────────────────────────────────────────
CREATE TABLE seats (
    id          BIGSERIAL PRIMARY KEY,
    screen_id   BIGINT NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    seat_number VARCHAR(10) NOT NULL,
    row         VARCHAR(5) NOT NULL,
    seat_index  INTEGER NOT NULL,
    category    VARCHAR(20) NOT NULL DEFAULT 'SILVER',
    UNIQUE (screen_id, seat_number)
);
CREATE INDEX idx_seats_screen   ON seats(screen_id);
CREATE INDEX idx_seats_category ON seats(category);

-- ─────────────────────────────────────────────
-- Movies
-- ─────────────────────────────────────────────
CREATE TABLE movies (
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    director          VARCHAR(255),
    movie_cast        TEXT,
    duration_minutes  INTEGER,
    language          VARCHAR(50),
    genre             VARCHAR(100),
    poster_url        VARCHAR(500),
    trailer_url       VARCHAR(500),
    certification     VARCHAR(10),
    status            VARCHAR(30) NOT NULL DEFAULT 'UPCOMING',
    release_date      TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_movies_status   ON movies(status);
CREATE INDEX idx_movies_language ON movies(language);
CREATE INDEX idx_movies_title    ON movies USING gin (title gin_trgm_ops);

-- ─────────────────────────────────────────────
-- Shows
-- ─────────────────────────────────────────────
CREATE TABLE shows (
    id               BIGSERIAL PRIMARY KEY,
    movie_id         BIGINT NOT NULL REFERENCES movies(id),
    screen_id        BIGINT NOT NULL REFERENCES screens(id),
    show_time        TIMESTAMP NOT NULL,
    show_end_time    TIMESTAMP NOT NULL,
    status           VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    silver_price     DECIMAL(10,2),
    gold_price       DECIMAL(10,2),
    platinum_price   DECIMAL(10,2),
    recline_price    DECIMAL(10,2),
    available_seats  INTEGER NOT NULL DEFAULT 0,
    total_seats      INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_shows_movie      ON shows(movie_id);
CREATE INDEX idx_shows_screen     ON shows(screen_id);
CREATE INDEX idx_shows_showtime   ON shows(show_time);
CREATE INDEX idx_shows_status     ON shows(status);
-- Composite index for the main browse query
CREATE INDEX idx_shows_browse ON shows(movie_id, show_time, status);

-- ─────────────────────────────────────────────
-- Show Seats (inventory per show)
-- ─────────────────────────────────────────────
CREATE TABLE show_seats (
    id              BIGSERIAL PRIMARY KEY,
    show_id         BIGINT NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_id         BIGINT NOT NULL REFERENCES seats(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version         BIGINT NOT NULL DEFAULT 0,  -- optimistic locking
    locked_at       TIMESTAMP,
    locked_by_user  VARCHAR(100),
    UNIQUE (show_id, seat_id)
);
CREATE INDEX idx_show_seats_show   ON show_seats(show_id);
CREATE INDEX idx_show_seats_status ON show_seats(show_id, status);

-- ─────────────────────────────────────────────
-- Offers
-- ─────────────────────────────────────────────
CREATE TABLE offers (
    id                      BIGSERIAL PRIMARY KEY,
    offer_code              VARCHAR(50) NOT NULL UNIQUE,
    description             TEXT,
    offer_type              VARCHAR(50) NOT NULL,
    discount_percentage     DECIMAL(5,2),
    max_discount_amount     DECIMAL(10,2),
    applicable_cities       TEXT,
    applicable_theatre_ids  JSONB,
    valid_from              TIMESTAMP,
    valid_to                TIMESTAMP,
    is_active               BOOLEAN DEFAULT TRUE
);

-- ─────────────────────────────────────────────
-- Bookings
-- ─────────────────────────────────────────────
CREATE TABLE bookings (
    id                BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(20) NOT NULL UNIQUE,
    user_id           BIGINT NOT NULL REFERENCES users(id),
    show_id           BIGINT NOT NULL REFERENCES shows(id),
    total_tickets     INTEGER NOT NULL,
    base_amount       DECIMAL(10,2) NOT NULL,
    discount_amount   DECIMAL(10,2) DEFAULT 0,
    convenience_fee   DECIMAL(10,2) DEFAULT 0,
    tax_amount        DECIMAL(10,2) DEFAULT 0,
    total_amount      DECIMAL(10,2) NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    payment_id        VARCHAR(100),
    payment_gateway   VARCHAR(50),
    payment_status    VARCHAR(30) DEFAULT 'PENDING',
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_bookings_user      ON bookings(user_id);
CREATE INDEX idx_bookings_show      ON bookings(show_id);
CREATE INDEX idx_bookings_reference ON bookings(booking_reference);
CREATE INDEX idx_bookings_status    ON bookings(status);

-- ─────────────────────────────────────────────
-- Booking Items
-- ─────────────────────────────────────────────
CREATE TABLE booking_items (
    id               BIGSERIAL PRIMARY KEY,
    booking_id       BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    show_seat_id     BIGINT NOT NULL REFERENCES show_seats(id),
    price            DECIMAL(10,2) NOT NULL,
    discount_applied DECIMAL(10,2) DEFAULT 0,
    discount_reason  VARCHAR(100)
);
CREATE INDEX idx_booking_items_booking ON booking_items(booking_id);

-- ─────────────────────────────────────────────
-- Seed Data
-- ─────────────────────────────────────────────
INSERT INTO offers (offer_code, description, offer_type, discount_percentage, is_active, valid_from, valid_to)
VALUES
  ('THIRD50',     '50% off on every 3rd ticket in a booking',     'THIRD_TICKET_50_PERCENT',  50.00, TRUE, NOW(), NOW() + INTERVAL '1 year'),
  ('AFTERNOON20', '20% off on all afternoon shows (12PM - 5PM)',  'AFTERNOON_20_PERCENT',     20.00, TRUE, NOW(), NOW() + INTERVAL '1 year');
