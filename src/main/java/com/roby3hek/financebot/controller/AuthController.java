package com.roby3hek.financebot.controller;

import com.roby3hek.financebot.dto.JwtResponse;
import com.roby3hek.financebot.service.JwtService;
import com.roby3hek.financebot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/login/{telegramId}")
    public JwtResponse login(@PathVariable Long telegramId) {
        // В реальном проекте здесь должна быть проверка, что запрос пришёл от бота
        String token = jwtService.generateToken(telegramId);
        return new JwtResponse(token);
    }
}