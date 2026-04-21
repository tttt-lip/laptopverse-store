package lipari.academy.com.laptopverse.modules.user.services.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lipari.academy.com.laptopverse.config.JwtProperties;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lipari.academy.com.laptopverse.modules.user.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    private static final String ACCESS_TOKEN_TYPE = "accessToken";
    private static final String REFRESH_TOKEN_TYPE = "refreshToken";

    private final JwtProperties jwtProperties;

    private Key getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        return generateToken(user, ACCESS_TOKEN_TYPE, jwtProperties.getTimeExpiredAccessMs());
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, REFRESH_TOKEN_TYPE, jwtProperties.getTimeExpiredRefreshMs());
    }

    private String generateToken(User user, String type, Long time_expiration) {
        var builder = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("type", type)
                .claim("userId", user.getId().toString())
                .claim("jti", UUID.randomUUID().toString());

        if (user.getRole() != null) {
            builder.claim("role", user.getRole().name());
        }

        return builder
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + time_expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public boolean isTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        final String type = extractTokenType(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token) && ACCESS_TOKEN_TYPE.equals(type));
    }

    public boolean isRefreshTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        final String type = extractTokenType(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token) && REFRESH_TOKEN_TYPE.equals(type));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        Instant expiration = extractClaim(token, Claims::getExpiration).toInstant();
        return expiration.isBefore(Instant.now());
    }
}
