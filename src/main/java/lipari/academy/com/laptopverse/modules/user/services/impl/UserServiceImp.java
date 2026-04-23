package lipari.academy.com.laptopverse.modules.user.services.impl;

import lipari.academy.com.laptopverse.config.JwtProperties;
import lipari.academy.com.laptopverse.exception.DuplicateResourceException;
import lipari.academy.com.laptopverse.exception.ResourceNotFoundException;
import lipari.academy.com.laptopverse.modules.user.dto.*;
import lipari.academy.com.laptopverse.modules.user.mapper.UserMapper;
import lipari.academy.com.laptopverse.modules.user.model.User;
import lipari.academy.com.laptopverse.modules.user.model.UserRole;
import lipari.academy.com.laptopverse.modules.user.repository.UserRepository;
import lipari.academy.com.laptopverse.modules.user.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImp implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public UserResponseDTO register(UserRequestDTO dto, UserDetails currentUser) {
        if (userRepository.existsByEmail(dto.email())) {
            throw DuplicateResourceException.emailAlreadyInUse(dto.email());
        }

        UserRole finalRole = dto.role() != null ? dto.role() : UserRole.CUSTOMER;

        if (currentUser == null && UserRole.ADMIN == finalRole) {
            throw new AccessDeniedException("You are not Admin");
        }
        User user = userMapper.toEntity(dto);
        user.setRole(finalRole);
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setEnabled(true);

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
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
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

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUser() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void softDeleteById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setEnabled(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponseDTO updateUserById(UUID userId, UserUpdateDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (dto.email() != null && !user.getEmail().equalsIgnoreCase(dto.email()) && userRepository.existsByEmail(dto.email())) {
            throw DuplicateResourceException.emailAlreadyInUse(dto.email());
        }

        userMapper.updateEntityFromDto(dto, user);

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }
}
