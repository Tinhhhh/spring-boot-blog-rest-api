package com.springboot.blog.service;

import com.springboot.blog.model.payload.dto.LoginDto;
import com.springboot.blog.model.payload.dto.RegisterDto;

public interface AuthService {
    String login(LoginDto loginDto);

    String register(RegisterDto registerDto);
}
