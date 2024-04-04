package com.springboot.blog.service.impl;

import com.springboot.blog.model.entity.AccessToken;
import com.springboot.blog.model.entity.RefreshToken;
import com.springboot.blog.repository.AccessTokenRepository;
import com.springboot.blog.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

    private final AccessTokenRepository accessTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        jwt = authHeader.substring(7);
        AccessToken accessToken = accessTokenRepository.findByToken(jwt);
        RefreshToken refreshToken = accessToken.getRefreshToken();

        if (refreshToken != null) {
            refreshToken.setRevoked(true);
            refreshToken.setExpired(true);
            refreshTokenRepository.save(refreshToken);
        }

        if (accessToken != null) {
            accessToken.setExpired(true);
            accessToken.setRevoked(true);
            accessTokenRepository.save(accessToken);
        }

    }
}
