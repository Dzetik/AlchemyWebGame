package com.example.demo.service;

import com.example.demo.dto.game.GameSessionPlayerResponse;
import com.example.demo.dto.game.GameSessionResponse;
import com.example.demo.entity.GameSession;
import com.example.demo.entity.GameSessionStatus;
import com.example.demo.event.GameUpdatedEvent;
import com.example.demo.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameSessionService {
    private final GameSessionAccessService gameSessionAccessService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public GameSessionResponse getGameSessionById(Long gameSessionId, Long userId) {
        GameSession gameSession = gameSessionAccessService.getGameSessionForPlayer(gameSessionId, userId);
        return toResponse(gameSession);
    }

    @Transactional
    public GameSessionResponse finishGame(Long gameSessionId, Long userId) {
        GameSession gameSession = gameSessionAccessService.getGameSessionForPlayer(gameSessionId, userId);

        if (gameSession.getStatus() != GameSessionStatus.ACTIVE) {
            throw new ConflictException("Завершить можно только активную игру");
        }

        gameSession.setStatus(GameSessionStatus.FINISHED);
        GameSessionResponse response = toResponse(gameSession);

        eventPublisher.publishEvent(
                new GameUpdatedEvent(
                        "GAME_FINISHED",
                        gameSession.getId(),
                        response
                )
        );

        return response;
    }

    public GameSessionResponse toResponse(GameSession gameSession) {
        List<GameSessionPlayerResponse> players = gameSession.getPlayers()
                .stream()
                .map(player -> new GameSessionPlayerResponse(
                        player.getUser().getId(),
                        player.getUser().getName()
                ))
                .toList();

        return new GameSessionResponse(
                gameSession.getId(),
                gameSession.getLobby().getId(),
                players,
                gameSession.getStatus(),
                gameSession.getStartedAt()
        );
    }
}