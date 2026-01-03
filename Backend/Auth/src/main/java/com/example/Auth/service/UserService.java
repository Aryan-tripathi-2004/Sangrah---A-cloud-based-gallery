package com.example.Auth.service;

import com.example.Auth.dto.RegisterRequest;
import com.example.Auth.dto.RegisterResponse;
import com.example.Auth.entity.OtpStore;
import com.example.Auth.entity.Role;
import com.example.Auth.entity.User;
import com.example.Auth.entity.UserProfile;
import com.example.Auth.event.UserRegisteredEvent;
import com.example.Auth.repository.RoleRepository;
import com.example.Auth.repository.UserProfileRepository;
import com.example.Auth.repository.UserRepository;
import com.example.Auth.common.exception.ResourceAlreadyExistsException;
import com.example.Auth.common.logging.DashLogger;
import com.example.Auth.common.logging.DashLoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static com.example.Auth.util.MessageConstants.*;

/**
 * Service for user management operations.
 */
@Service
public class UserService {

    private static final DashLogger logger = DashLoggerFactory.getLogger(UserService.class);
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            OtpService otpService,
            EmailService emailService,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registers a new user with email verification.
     *
     * @param request The registration request containing user details
     * @return The registration response with user ID and verification status
     * @throws ResourceAlreadyExistsException if username or email already exists
     */
    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        logger.info(MSG_REGISTERING_NEW_USER, Map.of(
                "username", request.getUsername(),
                "email", request.getEmail()));

        // Check if username already exists
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            logger.warn(MSG_USERNAME_ALREADY_EXISTS, Map.of("username", request.getUsername()));
            throw new ResourceAlreadyExistsException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            logger.warn(MSG_EMAIL_ALREADY_EXISTS, Map.of("email", request.getEmail()));
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        // Create User entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setProvider(User.AuthProvider.LOCAL);
        user.setEnabled(true);
        user.setLocked(false);

        // Get or create default role
        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseGet(() -> createDefaultRole());

        user.setRoles(Set.of(defaultRole));

        // Save user
        User savedUser = userRepository.save(user);

        logger.info(MSG_USER_CREATED_SUCCESS, Map.of(
                "userId", savedUser.getId().toString(),
                "username", savedUser.getUsername()));

        // Create UserProfile
        UserProfile userProfile = new UserProfile();
        userProfile.setUser(savedUser);
        userProfile.setFirstName(request.getFirstName());
        userProfile.setLastName(request.getLastName());
        userProfile.setDisplayName(request.getDisplayName() != null
                ? request.getDisplayName()
                : determineDisplayName(request));
        userProfile.setMarketingOptIn(request.getMarketingOptIn() != null
                ? request.getMarketingOptIn()
                : false);

        userProfileRepository.save(userProfile);

        logger.info(MSG_USER_PROFILE_CREATED, Map.of("userId", savedUser.getId().toString()));

        // Generate OTP for email verification
        String otpCode = otpService.generateOtp(
                savedUser.getId(),
                savedUser.getEmail(),
                OtpStore.Purpose.EMAIL_VERIFICATION);

        // Send verification email asynchronously
        try {
            emailService.sendVerificationEmail(
                    savedUser.getEmail(),
                    userProfile.getDisplayName(),
                    otpCode);
        } catch (Exception e) {
            logger.error(MSG_FAILED_SEND_VERIFICATION_EMAIL, Map.of(
                    "userId", savedUser.getId().toString(),
                    "email", savedUser.getEmail(),
                    "error", e.getMessage()));
            // Don't fail registration if email sending fails
        }

        // Publish UserRegisteredEvent for async processing
        UserRegisteredEvent event = new UserRegisteredEvent(
                savedUser.getId().toString(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                userProfile.getDisplayName(),
                true, // requiresEmailVerification
                otpCode);
        eventPublisher.publishEvent(event);

        logger.info(MSG_USER_REGISTERED_SUCCESS, Map.of(
                "userId", savedUser.getId().toString(),
                "username", savedUser.getUsername(),
                "email", savedUser.getEmail()));

        // Build and return response
        return RegisterResponse.builder()
                .userId(savedUser.getId().toString())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .message("Registration successful. Please check your email for verification code.")
                .requiresEmailVerification(true)
                .registeredAt(Instant.now())
                .build();
    }

    /**
     * Creates the default user role if it doesn't exist.
     *
     * @return The created default role
     */
    private Role createDefaultRole() {
        logger.info(MSG_CREATING_DEFAULT_ROLE, Map.of("roleName", DEFAULT_ROLE));

        Role role = new Role();
        role.setName(DEFAULT_ROLE);
        role.setDescription("Default role for registered users");

        return roleRepository.save(role);
    }

    /**
     * Determines the display name from the registration request.
     * Priority: displayName > firstName lastName > firstName > username
     *
     * @param request The registration request
     * @return The determined display name
     */
    private String determineDisplayName(RegisterRequest request) {
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            return request.getDisplayName();
        }

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            if (request.getLastName() != null && !request.getLastName().isBlank()) {
                return request.getFirstName() + " " + request.getLastName();
            }
            return request.getFirstName();
        }

        return request.getUsername();
    }

    /**
     * Verifies a user's email using the OTP code.
     * Note: User entity doesn't have email verification tracking fields yet.
     * This method verifies the OTP and can be extended later when verification
     * fields are added.
     *
     * @param email   The user's email
     * @param otpCode The OTP code
     * @return true if verification successful
     */
    @Transactional
    public boolean verifyEmail(String email, String otpCode) {
        logger.info(MSG_VERIFYING_EMAIL, Map.of("email", email));

        // Verify OTP
        boolean otpValid = otpService.verifyOtp(email, otpCode, OtpStore.Purpose.EMAIL_VERIFICATION);

        if (!otpValid) {
            logger.warn(MSG_INVALID_OTP_EMAIL_VERIFICATION, Map.of("email", email));
            return false;
        }

        // Find user by email
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // TODO: Update user's email verified status when fields are added to User
        // entity
        // user.setEmailVerified(true);
        // user.setEmailVerifiedAt(Instant.now());
        // userRepository.save(user);

        logger.info(MSG_EMAIL_VERIFIED_SUCCESS, Map.of(
                "userId", user.getId().toString(),
                "email", email));

        return true;
    }

    /**
     * Checks if a username is available.
     *
     * @param username The username to check
     * @return true if available, false otherwise
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsernameIgnoreCase(username);
    }

    /**
     * Checks if an email is available.
     *
     * @param email The email to check
     * @return true if available, false otherwise
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmailIgnoreCase(email);
    }
}
