package com.example.demo.service;

import com.example.demo.dto.game.CardDrawResponse;
import com.example.demo.dto.game.GameStateResponse;
import com.example.demo.dto.game.TurnResponse;
import com.example.demo.entity.*;
import com.example.demo.event.GameUpdatedEvent;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.GameStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class GameStateService {
    private final GameStateRepository gameStateRepository;
    private final GameSessionAccessService gameSessionAccessService;
    private final ApplicationEventPublisher eventPublisher;
    private final CardDrawService cardDrawService;

    @Transactional
    public GameState createInitialState(GameSession gameSession) {
        GameState gameState = new GameState();

        List<GameSessionPlayer> players = gameSession.getPlayers();
        GameSessionPlayer firstPlayer = players.get(ThreadLocalRandom.current().nextInt(players.size()));

        gameState.setCurrentPlayer(firstPlayer.getUser());
        gameState.setTurnNumber(1);

        gameState.setGameSession(gameSession);
        gameState.setCreatedAt(LocalDateTime.now());

        return gameStateRepository.save(gameState);
    }

    @Transactional(readOnly = true)
    public GameStateResponse getState(Long gameSessionId, Long userId) {
        gameSessionAccessService.getGameSessionForPlayer(gameSessionId, userId);

        GameState gameState = gameStateRepository.findByGameSessionId(gameSessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Состояние игры для gameSession " + gameSessionId + " не найдено")
                );

        return toResponse(gameState);
    }

    @Transactional(readOnly = true)
    public TurnResponse getTurn(Long gameId, Long userId) {
        gameSessionAccessService.getGameSessionForPlayer(gameId, userId);

        GameState state = gameStateRepository.findByGameSessionId(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Состояние игры для игровой сессии " + gameId + " не найдено")
                );

        return new TurnResponse(
                gameId,
                state.getCurrentPlayer().getId(),
                state.getTurnNumber()
        );
    }

    @Transactional
    public TurnResponse endTurn(Long gameId, Long userId) {
        GameSession game = gameSessionAccessService.getGameSessionForPlayer(gameId, userId);

        if (game.getStatus() != GameSessionStatus.ACTIVE) {
            throw new ConflictException("Игра не запущена");
        }

        GameState state = gameStateRepository
                .findByGameSessionIdForUpdate(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Состояние игры для игровой сессии " + gameId + " не найдено")
                );

        if (!state.getCurrentPlayer().getId().equals(userId)) {
            throw new ConflictException("Сейчас не ваш ход");
        }

        List<GameSessionPlayer> players = game.getPlayers().stream()
                .sorted(Comparator.comparing(GameSessionPlayer::getId))
                .toList();

        int currentIndex = -1;

        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUser().getId().equals(state.getCurrentPlayer().getId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1 || players.isEmpty()) {
            throw new ConflictException("Текущий игрок не является участником этой игры");
        }

        int nextIndex = (currentIndex + 1) % players.size();
        User nextPlayer = players.get(nextIndex).getUser();

        state.setCurrentPlayer(nextPlayer);
        state.setTurnNumber(state.getTurnNumber() + 1);

        // добор карт следующего игрока
        cardDrawService.drawCardsToInitialHand(game, nextPlayer);

        gameStateRepository.save(state);

        TurnResponse response = new TurnResponse(
                gameId,
                state.getCurrentPlayer().getId(),
                state.getTurnNumber()
        );

        eventPublisher.publishEvent(
                new GameUpdatedEvent("TURN_CHANGED", gameId, response)
        );

        return response;
    }

    private GameStateResponse toResponse(GameState gameState) {
        return new GameStateResponse(
                gameState.getId(),
                gameState.getGameSession().getId(),
                gameState.getRevision(),
                gameState.getCreatedAt()
        );
    }
}
