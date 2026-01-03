package com.example.Auth.controller;

import com.example.Auth.dto.LoginRequest;
import com.example.Auth.dto.LoginResponse;
import com.example.Auth.dto.RefreshTokenRequest;
import com.example.Auth.dto.RefreshTokenResponse;
import com.example.Auth.dto.RegisterRequest;
import com.example.Auth.dto.RegisterResponse;
import com.example.Auth.service.AuthenticationService;
import com.example.Auth.service.UserService;
import com.example.Auth.common.dto.ResponseWrapper;
import com.example.Auth.common.exception.AuthenticationException;
import com.example.Auth.common.logging.DashLogger;
import com.example.Auth.common.logging.DashLoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.Auth.util.ApiDocConstants.*;
import static com.example.Auth.util.MessageConstants.*;
import static com.example.Auth.util.NumericConstants.*;

/**
 * REST controller for authentication-related endpoints.
 */
@Tag(name = TAG_AUTHENTICATION, description = TAG_AUTHENTICATION_DESC)
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

        private static final DashLogger logger = DashLoggerFactory.getLogger(AuthController.class);

        private final UserService userService;
        private final AuthenticationService authenticationService;

        public AuthController(UserService userService, AuthenticationService authenticationService) {
                this.userService = userService;
                this.authenticationService = authenticationService;
        }

        /**
         * Registers a new user.
         *
         * @param request       The registration request
         * @param bindingResult The validation result
         * @return The registration response
         */
        @Operation(summary = OP_REGISTER_SUMMARY, description = OP_REGISTER_DESC)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = HTTP_201, description = RESP_REGISTER_201_DESC, content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
                        @ApiResponse(responseCode = HTTP_400, description = RESP_REGISTER_400_DESC),
                        @ApiResponse(responseCode = HTTP_500, description = RESP_REGISTER_500_DESC)
        })
        @PostMapping("/register")
        public ResponseEntity<ResponseWrapper<RegisterResponse>> register(
                        @Valid @RequestBody RegisterRequest request,
                        BindingResult bindingResult) {

                logger.info(MSG_REGISTRATION_REQUEST_RECEIVED, Map.of(
                                LOG_FIELD_USERNAME, request.getUsername(),
                                LOG_FIELD_EMAIL, request.getEmail()));

                // Check for validation errors
                if (bindingResult.hasErrors()) {
                        String errors = bindingResult.getAllErrors().stream()
                                        .map(error -> error.getDefaultMessage())
                                        .collect(Collectors.joining(", "));

                        logger.warn(MSG_REGISTRATION_VALIDATION_FAILED, Map.of(
                                        LOG_FIELD_USERNAME, request.getUsername(),
                                        LOG_FIELD_ERRORS, errors));

                        return ResponseEntity.badRequest().body(
                                        ResponseWrapper.error(MSG_VALIDATION_FAILED_PREFIX + errors,
                                                        HTTP_STATUS_BAD_REQUEST));
                }

                try {
                        RegisterResponse response = userService.registerUser(request);

                        logger.info(MSG_USER_REGISTERED_SUCCESS, Map.of(
                                        LOG_FIELD_USER_ID, response.getUserId(),
                                        LOG_FIELD_USERNAME, response.getUsername()));

                        return ResponseEntity.status(HttpStatus.CREATED).body(
                                        ResponseWrapper.success(response, MSG_USER_REGISTERED_SUCCESS));

                } catch (Exception e) {
                        logger.error(MSG_REGISTRATION_FAILED, Map.of(
                                        LOG_FIELD_USERNAME, request.getUsername(),
                                        LOG_FIELD_ERROR, e.getMessage()));

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        ResponseWrapper.error(MSG_REGISTRATION_FAILED + ": " + e.getMessage(),
                                                        HTTP_STATUS_INTERNAL_SERVER_ERROR));
                }
        }

        /**
         * Checks if a username is available.
         *
         * @param username The username to check
         * @return Response indicating if username is available
         */
        @Operation(summary = OP_CHECK_USERNAME_SUMMARY, description = OP_CHECK_USERNAME_DESC)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = HTTP_200, description = RESP_CHECK_USERNAME_200_DESC)
        })
        @GetMapping("/check-username")
        public ResponseEntity<ResponseWrapper<Map<String, Boolean>>> checkUsername(
                        @Parameter(description = PARAM_USERNAME_DESC, required = true, example = PARAM_USERNAME_EXAMPLE) @RequestParam String username) {

                boolean available = userService.isUsernameAvailable(username);

                logger.debug(MSG_USERNAME_AVAILABILITY_CHECK, Map.of(
                                LOG_FIELD_USERNAME, username,
                                LOG_FIELD_AVAILABLE, available));

                return ResponseEntity.ok(ResponseWrapper.success(Map.of(FIELD_AVAILABLE, available),
                                available ? MSG_USERNAME_AVAILABLE : MSG_USERNAME_TAKEN));
        }

        /**
         * Checks if an email is available.
         *
         * @param email The email to check
         * @return Response indicating if email is available
         */
        @Operation(summary = OP_CHECK_EMAIL_SUMMARY, description = OP_CHECK_EMAIL_DESC)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = HTTP_200, description = RESP_CHECK_EMAIL_200_DESC)
        })
        @GetMapping("/check-email")
        public ResponseEntity<ResponseWrapper<Map<String, Boolean>>> checkEmail(
                        @Parameter(description = PARAM_EMAIL_DESC, required = true, example = PARAM_EMAIL_EXAMPLE) @RequestParam String email) {

                boolean available = userService.isEmailAvailable(email);

                logger.debug(MSG_EMAIL_AVAILABILITY_CHECK, Map.of(
                                LOG_FIELD_EMAIL, email,
                                LOG_FIELD_AVAILABLE, available));

                return ResponseEntity.ok(ResponseWrapper.success(Map.of(FIELD_AVAILABLE, available),
                                available ? MSG_EMAIL_AVAILABLE : MSG_EMAIL_REGISTERED));
        }

        /**
         * Verifies a user's email with OTP code.
         *
         * @param email   The email address
         * @param otpCode The OTP code
         * @return Response indicating verification status
         */
        @Operation(summary = OP_VERIFY_EMAIL_SUMMARY, description = OP_VERIFY_EMAIL_DESC)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = HTTP_200, description = RESP_VERIFY_EMAIL_200_DESC),
                        @ApiResponse(responseCode = HTTP_400, description = RESP_VERIFY_EMAIL_400_DESC),
                        @ApiResponse(responseCode = HTTP_500, description = RESP_VERIFY_EMAIL_500_DESC)
        })
        @PostMapping("/verify-email")
        public ResponseEntity<ResponseWrapper<Map<String, Boolean>>> verifyEmail(
                        @Parameter(description = PARAM_EMAIL_DESC, required = true, example = PARAM_EMAIL_EXAMPLE) @RequestParam String email,
                        @Parameter(description = PARAM_OTP_DESC, required = true, example = PARAM_OTP_EXAMPLE) @RequestParam String otpCode) {

                logger.info(MSG_EMAIL_VERIFICATION_REQUEST, Map.of(LOG_FIELD_EMAIL, email));

                try {
                        boolean verified = userService.verifyEmail(email, otpCode);

                        if (verified) {
                                logger.info(MSG_EMAIL_VERIFIED_SUCCESS, Map.of(LOG_FIELD_EMAIL, email));
                                return ResponseEntity.ok(
                                                ResponseWrapper.success(Map.of(FIELD_VERIFIED, true),
                                                                MSG_EMAIL_VERIFIED_SUCCESS));
                        } else {
                                logger.warn(MSG_EMAIL_VERIFICATION_FAILED_INVALID_OTP, Map.of(LOG_FIELD_EMAIL, email));
                                return ResponseEntity.badRequest().body(
                                                ResponseWrapper.error(MSG_INVALID_OTP, HTTP_STATUS_BAD_REQUEST));
                        }
                } catch (Exception e) {
                        logger.error(MSG_EMAIL_VERIFICATION_ERROR, Map.of(
                                        LOG_FIELD_EMAIL, email,
                                        LOG_FIELD_ERROR, e.getMessage()));

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        ResponseWrapper.error(MSG_VERIFICATION_FAILED_PREFIX + e.getMessage(),
                                                        HTTP_STATUS_INTERNAL_SERVER_ERROR));
                }
        }

        /**
         * Authenticates a user and returns JWT tokens.
         *
         * @param request       The login request
         * @param bindingResult The validation result
         * @param httpRequest   The HTTP request for extracting metadata
         * @return The login response with tokens
         */
        @Operation(summary = OP_LOGIN_SUMMARY, description = OP_LOGIN_DESC)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = HTTP_200, description = RESP_LOGIN_200_DESC, content = @Content(schema = @Schema(implementation = LoginResponse.class))),
                        @ApiResponse(responseCode = HTTP_400, description = RESP_LOGIN_400_DESC),
                        @ApiResponse(responseCode = HTTP_401, description = RESP_LOGIN_401_DESC),
                        @ApiResponse(responseCode = HTTP_500, description = RESP_LOGIN_500_DESC)
        })
        @PostMapping("/login")
        public ResponseEntity<ResponseWrapper<LoginResponse>> login(
                        @Valid @RequestBody LoginRequest request,
                        BindingResult bindingResult,
                        HttpServletRequest httpRequest) {

                logger.info(MSG_LOGIN_REQUEST_RECEIVED, Map.of(
                                LOG_FIELD_USERNAME_OR_EMAIL, request.getUsernameOrEmail()));

                // Check for validation errors
                if (bindingResult.hasErrors()) {
                        String errors = bindingResult.getAllErrors().stream()
                                        .map(error -> error.getDefaultMessage())
                                        .collect(Collectors.joining(", "));

                        logger.warn(MSG_LOGIN_VALIDATION_FAILED, Map.of(
                                        LOG_FIELD_USERNAME_OR_EMAIL, request.getUsernameOrEmail(),
                                        LOG_FIELD_ERRORS, errors));

                        return ResponseEntity.badRequest().body(
                                        ResponseWrapper.error(MSG_VALIDATION_FAILED_PREFIX + errors,
                                                        HTTP_STATUS_BAD_REQUEST));
                }

                try {
                        LoginResponse response = authenticationService.login(request, httpRequest);

                        logger.info(MSG_USER_LOGGED_IN_SUCCESS, Map.of(
                                        LOG_FIELD_USER_ID, response.getUserId(),
                                        LOG_FIELD_USERNAME, response.getUsername(),
                                        LOG_FIELD_SESSION_ID, response.getSessionId()));

                        return ResponseEntity.ok(ResponseWrapper.success(response, MSG_LOGIN_SUCCESS));

                } catch (AuthenticationException e) {
                        logger.warn(MSG_LOGIN_FAILED, Map.of(
                                        LOG_FIELD_USERNAME_OR_EMAIL, request.getUsernameOrEmail(),
                                        LOG_FIELD_ERROR, e.getMessage()));

                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                        ResponseWrapper.error(e.getMessage(), HTTP_STATUS_UNAUTHORIZED));

                } catch (Exception e) {
                        logger.error(MSG_LOGIN_ERROR, Map.of(
                                        LOG_FIELD_USERNAME_OR_EMAIL, request.getUsernameOrEmail(),
                                        LOG_FIELD_ERROR, e.getMessage()));

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        ResponseWrapper.error(MSG_LOGIN_FAILED_PREFIX + e.getMessage(),
                                                        HTTP_STATUS_INTERNAL_SERVER_ERROR));
                }
        }

        /**
         * Refreshes an access token using a refresh token.
         *
         * @param request       The refresh token request
         * @param bindingResult The validation result
         * @return The refresh response with new access token
         */
        @Operation(summary = OP_REFRESH_SUMMARY, description = OP_REFRESH_DESC)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = HTTP_200, description = RESP_REFRESH_200_DESC, content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))),
                        @ApiResponse(responseCode = HTTP_400, description = RESP_REFRESH_400_DESC),
                        @ApiResponse(responseCode = HTTP_401, description = RESP_REFRESH_401_DESC),
                        @ApiResponse(responseCode = HTTP_500, description = RESP_REFRESH_500_DESC)
        })
        @PostMapping("/refresh")
        public ResponseEntity<ResponseWrapper<RefreshTokenResponse>> refreshToken(
                        @Valid @RequestBody RefreshTokenRequest request,
                        BindingResult bindingResult) {

                logger.info(MSG_TOKEN_REFRESH_REQUEST);

                // Check for validation errors
                if (bindingResult.hasErrors()) {
                        String errors = bindingResult.getAllErrors().stream()
                                        .map(error -> error.getDefaultMessage())
                                        .collect(Collectors.joining(", "));

                        logger.warn(MSG_TOKEN_REFRESH_VALIDATION_FAILED, Map.of(LOG_FIELD_ERRORS, errors));

                        return ResponseEntity.badRequest().body(
                                        ResponseWrapper.error(MSG_VALIDATION_FAILED_PREFIX + errors,
                                                        HTTP_STATUS_BAD_REQUEST));
                }

                try {
                        RefreshTokenResponse response = authenticationService.refreshAccessToken(request);

                        logger.info(MSG_TOKEN_REFRESHED_SUCCESS);

                        return ResponseEntity.ok(
                                        ResponseWrapper.success(response, MSG_TOKEN_REFRESHED_SUCCESS));

                } catch (AuthenticationException e) {
                        logger.warn(MSG_TOKEN_REFRESH_FAILED, Map.of(LOG_FIELD_ERROR, e.getMessage()));

                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                        ResponseWrapper.error(e.getMessage(), HTTP_STATUS_UNAUTHORIZED));

                } catch (Exception e) {
                        logger.error(MSG_TOKEN_REFRESH_ERROR, Map.of(LOG_FIELD_ERROR, e.getMessage()));

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        ResponseWrapper.error(MSG_TOKEN_REFRESH_FAILED_PREFIX + e.getMessage(),
                                                        HTTP_STATUS_INTERNAL_SERVER_ERROR));
                }
        }

        /**
         * Logs out a user by revoking their refresh token.
         *
         * @param request       The refresh token request
         * @param bindingResult The validation result
         * @return Response indicating logout status
         */
        @Operation(summary = OP_LOGOUT_SUMMARY, description = OP_LOGOUT_DESC)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = HTTP_200, description = RESP_LOGOUT_200_DESC),
                        @ApiResponse(responseCode = HTTP_400, description = RESP_LOGOUT_400_DESC)
        })
        @PostMapping("/logout")
        public ResponseEntity<ResponseWrapper<Map<String, Boolean>>> logout(
                        @Valid @RequestBody RefreshTokenRequest request,
                        BindingResult bindingResult) {

                logger.info(MSG_LOGOUT_REQUEST_RECEIVED);

                // Check for validation errors
                if (bindingResult.hasErrors()) {
                        String errors = bindingResult.getAllErrors().stream()
                                        .map(error -> error.getDefaultMessage())
                                        .collect(Collectors.joining(", "));

                        logger.warn(MSG_LOGOUT_VALIDATION_FAILED, Map.of(LOG_FIELD_ERRORS, errors));

                        return ResponseEntity.badRequest().body(
                                        ResponseWrapper.error(MSG_VALIDATION_FAILED_PREFIX + errors,
                                                        HTTP_STATUS_BAD_REQUEST));
                }

                try {
                        authenticationService.logout(request.getRefreshToken());

                        logger.info(MSG_USER_LOGGED_OUT_SUCCESS);

                        return ResponseEntity.ok(
                                        ResponseWrapper.success(Map.of(FIELD_SUCCESS, true), MSG_LOGGED_OUT_SUCCESS));

                } catch (Exception e) {
                        logger.error(MSG_LOGOUT_ERROR, Map.of(LOG_FIELD_ERROR, e.getMessage()));

                        // Return success even on error - logout should be idempotent
                        return ResponseEntity.ok(
                                        ResponseWrapper.success(Map.of(FIELD_SUCCESS, true), MSG_LOGGED_OUT_SUCCESS));
                }
        }

        /**
         * Logs out a user from all devices/sessions.
         * Requires authentication - extracts user ID from security context.
         *
         * @return Response indicating logout status
         */
        @Operation(summary = OP_LOGOUT_ALL_SUMMARY, description = OP_LOGOUT_ALL_DESC, security = @SecurityRequirement(name = OP_LOGOUT_ALL_SECURITY))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = HTTP_200, description = RESP_LOGOUT_ALL_200_DESC),
                        @ApiResponse(responseCode = HTTP_401, description = RESP_LOGOUT_ALL_401_DESC),
                        @ApiResponse(responseCode = HTTP_500, description = RESP_LOGOUT_ALL_500_DESC)
        })
        @PostMapping("/logout-all")
        public ResponseEntity<ResponseWrapper<Map<String, Boolean>>> logoutAll() {

                logger.info(MSG_LOGOUT_ALL_REQUEST_RECEIVED);

                try {
                        // Get authenticated user ID from security context
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                        if (authentication == null || !authentication.isAuthenticated()) {
                                logger.warn(MSG_LOGOUT_ALL_FAILED_NOT_AUTHENTICATED);
                                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                                ResponseWrapper.error(MSG_USER_NOT_AUTHENTICATED,
                                                                HTTP_STATUS_UNAUTHORIZED));
                        }

                        // Extract user ID from authentication principal (JWT subject)
                        String userIdString = authentication.getName();
                        UUID userId = UUID.fromString(userIdString);

                        authenticationService.logoutAll(userId);

                        logger.info(MSG_USER_LOGGED_OUT_ALL_SUCCESS, Map.of(LOG_FIELD_USER_ID, userIdString));

                        return ResponseEntity.ok(ResponseWrapper.success(Map.of(FIELD_SUCCESS, true),
                                        MSG_LOGGED_OUT_ALL_SUCCESS));

                } catch (Exception e) {
                        logger.error(MSG_LOGOUT_ALL_ERROR, Map.of(LOG_FIELD_ERROR, e.getMessage()));

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        ResponseWrapper.error(MSG_LOGOUT_ALL_FAILED_PREFIX + e.getMessage(),
                                                        HTTP_STATUS_INTERNAL_SERVER_ERROR));
                }
        }
}
