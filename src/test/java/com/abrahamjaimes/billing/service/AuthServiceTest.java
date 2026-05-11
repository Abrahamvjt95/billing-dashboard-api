package com.abrahamjaimes.billing.service;

import com.abrahamjaimes.billing.dto.request.LoginRequest;
import com.abrahamjaimes.billing.dto.request.RefreshTokenRequest;
import com.abrahamjaimes.billing.dto.request.RegisterRequest;
import com.abrahamjaimes.billing.dto.response.AuthResponse;
import com.abrahamjaimes.billing.entity.RefreshToken;
import com.abrahamjaimes.billing.entity.Role;
import com.abrahamjaimes.billing.entity.User;
import com.abrahamjaimes.billing.exception.ConflictException;
import com.abrahamjaimes.billing.exception.UnauthorizedException;
import com.abrahamjaimes.billing.repository.RefreshTokenRepository;
import com.abrahamjaimes.billing.repository.UserRepository;
import com.abrahamjaimes.billing.security.JwtProperties;
import com.abrahamjaimes.billing.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock JwtProperties jwtProperties;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L).email("abraham@test.com").password("hashed")
                .firstName("Abraham").lastName("Jaimes")
                .role(Role.USER).enabled(true).build();
    }

    @Test
    void register_success() {
        var request = new RegisterRequest("abraham@test.com", "password123", "Abraham", "Jaimes");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("abraham@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsConflict_whenEmailExists() {
        var request = new RegisterRequest("abraham@test.com", "password123", "Abraham", "Jaimes");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_success() {
        var request = new LoginRequest("abraham@test.com", "password123");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_throwsUnauthorized_whenBadCredentials() {
        var request = new LoginRequest("abraham@test.com", "wrong");
        doThrow(new BadCredentialsException("bad")).when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_success() {
        var token = RefreshToken.builder()
                .token("valid-token").user(sampleUser)
                .revoked(false).expiresAt(LocalDateTime.now().plusDays(7)).build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateAccessToken(any())).thenReturn("new-access");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);

        AuthResponse response = authService.refresh(new RefreshTokenRequest("valid-token"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void refresh_throwsUnauthorized_whenTokenExpired() {
        var token = RefreshToken.builder()
                .token("expired").user(sampleUser)
                .revoked(false).expiresAt(LocalDateTime.now().minusDays(1)).build();

        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_throwsUnauthorized_whenTokenRevoked() {
        var token = RefreshToken.builder()
                .token("revoked").user(sampleUser)
                .revoked(true).expiresAt(LocalDateTime.now().plusDays(7)).build();

        when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("revoked")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
