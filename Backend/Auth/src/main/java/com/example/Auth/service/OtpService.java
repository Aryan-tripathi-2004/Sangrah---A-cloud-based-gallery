package com.example.Auth.service;

import com.example.Auth.entity.OtpStore;
import com.example.Auth.exception.OtpException;
import com.example.Auth.repository.OtpStoreRepository;
import com.example.Auth.common.logging.DashLogger;
import com.example.Auth.common.logging.DashLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.example.Auth.util.MessageConstants.*;

/**
 * Service for OTP (One-Time Password) generation, verification, and rate
 * limiting.
 */
@Service
public class OtpService {

    private static final DashLogger logger = DashLoggerFactory.getLogger(OtpService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpStoreRepository otpStoreRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.validity-minutes:10}")
    private int otpValidityMinutes;

    @Value("${app.otp.max-attempts:10}")
    private int maxAttempts;

    @Value("${app.otp.rate-limit-per-hour:5}")
    private int rateLimitPerHour;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public OtpService(OtpStoreRepository otpStoreRepository,
            RedisTemplate<String, String> redisTemplate) {
        this.otpStoreRepository = otpStoreRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generates and stores a new OTP for the given email and purpose.
     *
     * @param userId  The user ID (null for pre-registration scenarios)
     * @param email   The email address
     * @param purpose The OTP purpose (EMAIL_VERIFICATION, PASSWORD_RESET, etc.)
     * @return The generated OTP code (to be sent via email)
     * @throws OtpException if rate limit is exceeded
     */
    @Transactional
    public String generateOtp(UUID userId, String email, OtpStore.Purpose purpose) {
        logger.info(MSG_GENERATING_OTP, Map.of(
                "userId", userId != null ? userId.toString() : "null",
                "email", email,
                "purpose", purpose.name()));

        // Check rate limit
        checkRateLimit(email, purpose);

        // Invalidate any existing OTPs for this email and purpose
        otpStoreRepository.deleteByEmailAndPurpose(email, purpose);

        // Generate random OTP code
        String otpCode = generateRandomOtpCode();

        // Generate HMAC hash of the OTP
        String otpHash = generateOtpHash(email, otpCode, purpose.name());

        // Calculate expiry time
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(otpValidityMinutes));

        // Create and save OTP entity
        OtpStore otpStore = new OtpStore();
        otpStore.setUserId(userId);
        otpStore.setEmail(email);
        otpStore.setOtpHash(otpHash);
        otpStore.setPurpose(purpose);
        otpStore.setExpiresAt(expiresAt);
        otpStore.setAttempts(0);

        otpStoreRepository.save(otpStore);

        // Increment rate limit counter
        incrementRateLimitCounter(email, purpose);

        logger.info(MSG_OTP_GENERATED_SUCCESS, Map.of(
                "userId", userId != null ? userId.toString() : "null",
                "email", email,
                "purpose", purpose.name(),
                "expiresAt", expiresAt.toString()));

        return otpCode;
    }

    /**
     * Verifies the provided OTP code for the given email and purpose.
     *
     * @param email   The email address
     * @param otpCode The OTP code to verify
     * @param purpose The OTP purpose
     * @return true if OTP is valid
     * @throws OtpException if OTP verification fails
     */
    @Transactional
    public boolean verifyOtp(String email, String otpCode, OtpStore.Purpose purpose) {
        logger.info(MSG_VERIFYING_OTP, Map.of(
                "email", email,
                "purpose", purpose.name()));

        // Find active OTP
        OtpStore otpStore = otpStoreRepository
                .findValidOtpByEmailAndPurpose(email, purpose, Instant.now())
                .orElseThrow(() -> new OtpException("Invalid or expired OTP"));

        // Check if max attempts exceeded
        if (otpStore.getAttempts() >= maxAttempts) {
            logger.warn(MSG_MAX_OTP_ATTEMPTS_EXCEEDED, Map.of(
                    "email", email,
                    "purpose", purpose.name(),
                    "attempts", otpStore.getAttempts()));
            throw new OtpException("Maximum OTP verification attempts exceeded");
        }

        // Increment attempt counter
        otpStore.setAttempts(otpStore.getAttempts() + 1);
        otpStoreRepository.save(otpStore);

        // Verify OTP hash
        String expectedHash = generateOtpHash(email, otpCode, purpose.name());
        if (!expectedHash.equals(otpStore.getOtpHash())) {
            logger.warn(MSG_INVALID_OTP_PROVIDED, Map.of(
                    "email", email,
                    "purpose", purpose.name(),
                    "attempts", otpStore.getAttempts()));
            return false;
        }

        // Mark OTP as used
        otpStore.setUsed(true);
        otpStore.setUsedAt(Instant.now());
        otpStoreRepository.save(otpStore);

        logger.info(MSG_OTP_VERIFIED_SUCCESS, Map.of(
                "email", email,
                "purpose", purpose.name()));

        return true;
    }

    /**
     * Checks if the user has exceeded the rate limit for OTP generation.
     *
     * @param email   The email address
     * @param purpose The OTP purpose
     * @throws OtpException if rate limit is exceeded
     */
    private void checkRateLimit(String email, OtpStore.Purpose purpose) {
        String rateLimitKey = getRateLimitKey(email, purpose);
        String countStr = redisTemplate.opsForValue().get(rateLimitKey);

        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            if (count >= rateLimitPerHour) {
                logger.warn(MSG_OTP_RATE_LIMIT_EXCEEDED, Map.of(
                        "email", email,
                        "purpose", purpose.name(),
                        "count", count,
                        "limit", rateLimitPerHour));
                throw new OtpException("OTP generation rate limit exceeded. Please try again later.");
            }
        }
    }

    /**
     * Increments the rate limit counter for OTP generation.
     *
     * @param email   The email address
     * @param purpose The OTP purpose
     */
    private void incrementRateLimitCounter(String email, OtpStore.Purpose purpose) {
        String rateLimitKey = getRateLimitKey(email, purpose);
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);

        if (count != null && count == 1) {
            // Set expiry for the first increment
            redisTemplate.expire(rateLimitKey, 1, TimeUnit.HOURS);
        }
    }

    /**
     * Generates a random OTP code.
     *
     * @return The generated OTP code
     */
    private String generateRandomOtpCode() {
        int bound = (int) Math.pow(10, otpLength);
        int randomNumber = SECURE_RANDOM.nextInt(bound);
        return String.format("%0" + otpLength + "d", randomNumber);
    }

    /**
     * Generates HMAC-SHA256 hash of OTP for secure storage.
     *
     * @param email   The email address
     * @param otpCode The OTP code
     * @param purpose The OTP purpose
     * @return Hex-encoded HMAC hash (64 characters)
     */
    private String generateOtpHash(String email, String otpCode, String purpose) {
        try {
            String data = email + ":" + otpCode + ":" + purpose;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    jwtSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string (64 characters for SHA-256)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error(MSG_FAILED_GENERATE_OTP_HASH, Map.of(
                    "error", e.getMessage()));
            throw new RuntimeException("Failed to generate OTP hash", e);
        }
    }

    /**
     * Generates Redis key for rate limiting.
     *
     * @param email   The email address
     * @param purpose The OTP purpose
     * @return The rate limit key
     */
    private String getRateLimitKey(String email, OtpStore.Purpose purpose) {
        return "otp:ratelimit:" + email + ":" + purpose.name();
    }

    /**
     * Deletes all expired OTPs from the database.
     * This method should be scheduled to run periodically.
     */
    @Transactional
    public void cleanupExpiredOtps() {
        logger.info(MSG_CLEANING_UP_EXPIRED_OTPS);
        otpStoreRepository.deleteByExpiresAtBefore(Instant.now());
        logger.info(MSG_EXPIRED_OTPS_CLEANUP_COMPLETED, Map.of("message", "Expired OTPs deleted"));
    }
}
