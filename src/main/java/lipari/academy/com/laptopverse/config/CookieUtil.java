package lipari.academy.com.laptopverse.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final JwtProperties jwtProperties;


    public ResponseCookie createAccessTokenCookie(String token) {
        return getResponseCookie("accessToken", token, "/", jwtProperties.getTimeExpiredAccessSeconds());
    }

    public ResponseCookie createRefershTokenCookie(String token) {
        return getResponseCookie("refreshToken", token, "/api/users/refresh", jwtProperties.getTimeExpiredRefreshSeconds());
    }

    public ResponseCookie deleteAccessTokenCookie() {
        return getResponseCookie("accessToken", "", "/", 0L);
    }

    public ResponseCookie deleteRefershTokenCookie() {
        return getResponseCookie("refreshToken", "", "/api/users/refresh", 0L);
    }

    public Optional<String> extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isEmpty())
                .findFirst();
    }

    private @NonNull ResponseCookie getResponseCookie(String type, String token, String path, Long ageSeconds) {
        return ResponseCookie.from(type, token)
                .httpOnly(jwtProperties.isHttpOnly())
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .path(path)
                .maxAge(ageSeconds)
                .build();
    }

}
