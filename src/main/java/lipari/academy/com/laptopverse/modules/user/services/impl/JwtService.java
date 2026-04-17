package lipari.academy.com.laptopverse.modules.user.services.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lipari.academy.com.laptopverse.config.JwtProperties;
import lipari.academy.com.laptopverse.modules.user.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    private Key getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String email, UserRole role) {
        return generateToken(email, role, "accessToken", jwtProperties.getTimeExpiredAccessMs());
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, null, "refreshToken", jwtProperties.getTimeExpiredRefreshMs());
    }

    private String generateToken(String email, UserRole role, String type, Long time_expiration) {
        var builder = Jwts.builder()
                .setSubject(email)
                .claim("type", type)
                .claim("jti", UUID.randomUUID().toString());

        if (role != null) {
            builder.claim("role", role.name());
        }

        return builder
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + time_expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    public String refreshToken(String oldToken) {
        final String email = extractEmail(oldToken);
        if (email == null) {
            throw new IllegalArgumentException("Refresh token non valido");
        }
        return generateRefreshToken(email);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public boolean isTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        final String type = extractTokenType(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token) && "access".equals(type));
    }

    public boolean isRefreshTokenValid(String token, String email) {
        final String extractedEmail = extractEmail(token);
        final String type = extractTokenType(token);
        return (extractedEmail.equals(email) && !isTokenExpired(token) && "refreshToken".equals(type));
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
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
