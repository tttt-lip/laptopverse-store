package lipari.academy.com.laptopverse.modules.user.services.impl;

import lipari.academy.com.laptopverse.config.JwtAuthenticationFilter;
import lipari.academy.com.laptopverse.config.JwtProperties;
import lipari.academy.com.laptopverse.exception.RefreshTokenException;
import lipari.academy.com.laptopverse.exception.ResourceNotFoundException;
import lipari.academy.com.laptopverse.modules.auth.dto.RefreshToken;
import lipari.academy.com.laptopverse.modules.auth.model.JwtTokenPair;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lipari.academy.com.laptopverse.modules.user.repository.RefreshTokenRepository;
import lipari.academy.com.laptopverse.modules.user.repository.UserRepository;
import lipari.academy.com.laptopverse.modules.user.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImp implements AuthService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    private final JwtProperties jwtProperties;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);


    @Override
    public JwtTokenPair generateTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .expiryDate(Instant.now().plusMillis(jwtProperties.getTimeExpiredRefreshMs())) // 7 giorni
                .user(user)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return new JwtTokenPair(accessToken, refreshToken);
    }

    @Transactional
    @Override
    public JwtTokenPair refreshToken(String oldRefershToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(oldRefershToken)
                .orElseThrow(() -> new RefreshTokenException("RefreshToken not found with token = " + oldRefershToken));

        if (!storedToken.isActive()) {
            throw new RefreshTokenException("Refresh token expired");
        }

        String email = jwtService.extractEmail(oldRefershToken);
        if (!jwtService.isRefreshTokenValid(oldRefershToken, email)) {
            throw new RefreshTokenException("Refresh token not valid");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .token(newRefreshToken)
                .expiryDate(Instant.now().plusMillis(jwtProperties.getTimeExpiredRefreshMs()))
                .user(user)
                .build();

        storedToken.revoke();

        refreshTokenRepository.save(storedToken);
        refreshTokenRepository.save(newRefreshTokenEntity);

        return new JwtTokenPair(newAccessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);

    }
}
