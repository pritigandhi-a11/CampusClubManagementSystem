-- =========================================================================
-- Campus Club Management System - Security & OTP Identity Migration
-- Institution: Kalasalingam Academy of Research and Education (@klu.ac.in)
-- Date: 2026-10-30
-- Target Table: users
-- =========================================================================

-- 1. Student verification status (Defaults to TRUE so all existing accounts remain active)
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT TRUE;

-- 2. 6-digit institutional OTP code
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_code VARCHAR(10) DEFAULT NULL;

-- 3. OTP 10-minute expiration timestamp
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_expires_at TIMESTAMP NULL DEFAULT NULL;

-- 4. Consecutive failed OTP verification attempts counter (locks at 5)
ALTER TABLE users ADD COLUMN IF NOT EXISTS otp_attempts INT DEFAULT 0;
