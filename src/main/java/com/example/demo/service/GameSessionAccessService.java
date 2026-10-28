package com.example.demo.service;

import com.example.demo.entity.GameSession;
import com.example.demo.exception.ForbiddenException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.GameSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameSessionAccessService {
    private final GameSessionRepository gameSessionRepository;

    @Transactional(readOnly = true)
    public GameSession getGameSessionForPlayer(Long gameId, Long userId) {
        GameSession gameSession = gameSessionRepository.findById(gameId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Игровая сессия с id " + gameId + " не найдена")
                );

        boolean isPlayer = gameSession.getPlayers()
                .stream()
                .anyMatch(player ->
                        player.getUser().getId().equals(userId)
                );

        if (!isPlayer) {
            throw new ForbiddenException("Пользователь не является участником этой игры");
        }

        return gameSession;
    }
}