package com.vitacare.api.controller;

import com.vitacare.api.model.Client;
import com.vitacare.api.model.User;
import com.vitacare.api.repository.ClientRepository;
import com.vitacare.api.repository.UserRepository;
import com.vitacare.api.security.AuthTokenService;
import com.vitacare.api.security.AuthenticatedUser;
import com.vitacare.api.security.RoleFlags;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
        UserRepository userRepository,
        ClientRepository clientRepository,
        AuthTokenService authTokenService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.authTokenService = authTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        int roleMask = normalizeRoles(request.roles(), request.roleMask(), RoleFlags.CLIENT);
        validateProfilePayload(roleMask, request);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoleMask(roleMask);

        User saved = userRepository.save(user);
        ProfileResponse clientProfile = null;
        ProfileResponse nutritionistProfile = null;

        if (RoleFlags.hasRole(roleMask, RoleFlags.CLIENT)) {
            Client client = new Client();
            client.setUser(saved);
            client.setName(request.client().name());
            Client savedClient = clientRepository.save(client);
            clientProfile = toProfileResponse(savedClient);
        }

        if (RoleFlags.hasRole(roleMask, RoleFlags.NUTRITIONIST)) {
            nutritionistProfile = toProfileResponse(saved.getId(), request.nutritionist());
        }

        String token = authTokenService.issueToken(saved);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new RegisterResponse(token, "Bearer", toMeResponse(saved, clientProfile, nutritionistProfile)));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = authTokenService.issueToken(user);
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", toAuthUserResponse(user)));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public MeResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        User user = userRepository.findById(principal.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ProfileResponse clientProfile = clientRepository.findByUser_Id(user.getId())
            .map(AuthController::toProfileResponse)
            .orElse(null);
        ProfileResponse nutritionistProfile = null;

        return toMeResponse(user, clientProfile, nutritionistProfile);
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static int normalizeRoles(List<String> roles, Integer explicitRoleMask, int fallback) {
        int byNames = RoleFlags.parseRoles(roles, fallback);
        if (explicitRoleMask == null) {
            return byNames;
        }

        int normalizedMask = explicitRoleMask;
        if (normalizedMask < 0 || normalizedMask > (RoleFlags.CLIENT | RoleFlags.NUTRITIONIST | RoleFlags.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid roleMask");
        }

        if (roles != null && !roles.isEmpty() && normalizedMask != byNames) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roles and roleMask do not match");
        }

        return normalizedMask;
    }

    private static void validateProfilePayload(int roleMask, RegisterRequest request) {
        boolean hasClientRole = RoleFlags.hasRole(roleMask, RoleFlags.CLIENT);
        boolean hasNutritionistRole = RoleFlags.hasRole(roleMask, RoleFlags.NUTRITIONIST);

        if (hasClientRole) {
            if (request.client() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "client payload is required for CLIENT role");
            }
        } else if (request.client() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "client payload must be null when CLIENT role is absent");
        }

        if (hasNutritionistRole) {
            if (request.nutritionist() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nutritionist payload is required for NUTRITIONIST role");
            }
        } else if (request.nutritionist() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nutritionist payload must be null when NUTRITIONIST role is absent");
        }
    }

    private static MeResponse toMeResponse(User user, ProfileResponse clientProfile, ProfileResponse nutritionistProfile) {
        int roleMask = user.getRoleMask() == null ? RoleFlags.CLIENT : user.getRoleMask();
        return new MeResponse(user.getId(), user.getEmail(), roleMask, RoleFlags.toNames(roleMask), clientProfile, nutritionistProfile);
    }

    private static AuthUserResponse toAuthUserResponse(User user) {
        int roleMask = user.getRoleMask() == null ? RoleFlags.CLIENT : user.getRoleMask();
        return new AuthUserResponse(user.getId(), user.getEmail(), roleMask, RoleFlags.toNames(roleMask));
    }

    private static ProfileResponse toProfileResponse(Client client) {
        return new ProfileResponse(client.getId(), client.getName(), null, "CLIENT");
    }

    private static ProfileResponse toProfileResponse(UUID userId, NutritionistRegisterPayload nutritionist) {
        return new ProfileResponse(userId, nutritionist.name(), nutritionist.description(), "NUTRITIONIST");
    }

    public record RegisterRequest(
        @NotBlank String email,
        @NotBlank String password,
        List<String> roles,
        Integer roleMask,
        @Valid ClientRegisterPayload client,
        @Valid NutritionistRegisterPayload nutritionist
    ) {
    }

    public record ClientRegisterPayload(@NotBlank String name) {
    }

    public record NutritionistRegisterPayload(@NotBlank String name, String description) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record RegisterResponse(String token, String type, MeResponse user) {
    }

    public record LoginResponse(String token, String type, AuthUserResponse user) {
    }

    public record AuthUserResponse(UUID id, String email, int roleMask, List<String> roles) {
    }

    public record MeResponse(UUID id, String email, int roleMask, List<String> roles, ProfileResponse client, ProfileResponse nutritionist) {
    }

    public record ProfileResponse(UUID id, String name, String description, String type) {
    }
}