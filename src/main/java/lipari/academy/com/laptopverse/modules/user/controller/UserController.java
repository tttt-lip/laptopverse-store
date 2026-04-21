package lipari.academy.com.laptopverse.modules.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lipari.academy.com.laptopverse.common.ApiResponse;
import lipari.academy.com.laptopverse.config.CookieUtil;
import lipari.academy.com.laptopverse.config.JwtProperties;
import lipari.academy.com.laptopverse.modules.auth.dto.LogoutResponse;
import lipari.academy.com.laptopverse.modules.auth.model.JwtTokenPair;
import lipari.academy.com.laptopverse.modules.user.dto.LoginRequest;
import lipari.academy.com.laptopverse.modules.user.dto.LoginResponse;
import lipari.academy.com.laptopverse.modules.user.dto.UserRequestDTO;
import lipari.academy.com.laptopverse.modules.user.dto.UserResponseDTO;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lipari.academy.com.laptopverse.modules.user.model.UserRole;
import lipari.academy.com.laptopverse.modules.user.services.AuthService;
import lipari.academy.com.laptopverse.modules.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtProperties jwtProperties;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(@Valid @RequestBody UserRequestDTO dto, @AuthenticationPrincipal User currentUser) {
        UserResponseDTO response = userService.register(dto, currentUser);
        return ResponseEntity.status(201).body(ApiResponse.success(response, "Created User"));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse httpServletResponse) {
        LoginResponse loginResponse = userService.login(loginRequest);

        User user = userService.findUserByEmail(loginResponse.email());
        JwtTokenPair jwtTokenPair = authService.generateTokens(user);

        log.debug(jwtTokenPair.newAccessToken().substring(30), "Token Access");
        log.debug(jwtTokenPair.newRefreshToken().substring(30), "Token Refresh");
        addCookieHeader(httpServletResponse, cookieUtil.createAccessTokenCookie(jwtTokenPair.newAccessToken()),
                cookieUtil.createRefershTokenCookie(jwtTokenPair.newRefreshToken()));

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshCookie(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String refreshToken = cookieUtil.extractTokenFromCookie(httpServletRequest, "refreshToken")
                .orElse("");
        if (refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        JwtTokenPair jwtTokenPair = authService.refreshToken(refreshToken);

        addCookieHeader(httpServletResponse, cookieUtil.createAccessTokenCookie(jwtTokenPair.newAccessToken()), cookieUtil.createRefershTokenCookie(jwtTokenPair.newRefreshToken()));

        return ResponseEntity.ok(LoginResponse.builder()
                .accesTokenExpiresIn(jwtProperties.getTimeExpiredAccessMs())
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        String refreshToken = cookieUtil.extractTokenFromCookie(httpServletRequest, "refreshToken")
                .orElse("");

        if (!refreshToken.isEmpty()) {
            authService.logout(refreshToken);
        }
        addCookieHeader(httpServletResponse, cookieUtil.deleteAccessTokenCookie(),
                cookieUtil.deleteRefershTokenCookie());

        return ResponseEntity.ok(new LogoutResponse("Logout successful."));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUser();
        return ResponseEntity.ok(ApiResponse.success(users, "All users found"));
    }

    private static void addCookieHeader(HttpServletResponse httpServletResponse, ResponseCookie cookieAccess, ResponseCookie cookieRefresh) {
        httpServletResponse.addHeader("Set-Cookie", cookieAccess.toString());
        httpServletResponse.addHeader("Set-Cookie", cookieRefresh.toString());
    }
}
