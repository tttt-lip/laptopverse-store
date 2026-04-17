package lipari.academy.com.laptopverse.modules.user.services.impl;

import lipari.academy.com.laptopverse.config.JwtProperties;
import lipari.academy.com.laptopverse.exception.DuplicateResourceException;
import lipari.academy.com.laptopverse.modules.user.dto.LoginRequest;
import lipari.academy.com.laptopverse.modules.user.dto.LoginResponse;
import lipari.academy.com.laptopverse.modules.user.dto.UserRequestDTO;
import lipari.academy.com.laptopverse.modules.user.dto.UserResponseDTO;
import lipari.academy.com.laptopverse.modules.user.mapper.UserMapper;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lipari.academy.com.laptopverse.modules.user.repository.UserRepository;
import lipari.academy.com.laptopverse.modules.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImp implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private final JwtProperties jwtProperties;

    @Override
    public UserResponseDTO register(UserRequestDTO dto) {
        if (userRepository.existsBydisplayName(dto.displayName())) {
            throw DuplicateResourceException.usernameAlreadyInUse(dto.displayName());
        }
        if (userRepository.existsByEmail(dto.email())) {
            throw DuplicateResourceException.emailAlreadyInUse(dto.email());
        }
        User user = userMapper.toEntity(dto);

        String encodedPassword = passwordEncoder.encode(dto.password());

        user.setPassword(encodedPassword);

        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("Credenziali non valide"));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new BadCredentialsException("Credenziali non valide");
        }
        return LoginResponse.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .role(user.getRole())
                .accesTokenExpiresIn(jwtProperties.getTimeExpiredAccessSeconds())
                .build();
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email = " + email));
    }
}
