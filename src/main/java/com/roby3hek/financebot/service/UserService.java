package com.roby3hek.financebot.service;

import com.roby3hek.financebot.entity.User;
import com.roby3hek.financebot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findOrCreate(Long telegramId, String username) {
        return userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .telegramId(telegramId)
                                .username(username)
                                .createdAt(LocalDateTime.now())
                                .build()
                ));
    }

    public User findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}