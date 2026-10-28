package com.example.demo.service;

import com.example.demo.dto.lobby.CreateLobbyRequest;
import com.example.demo.dto.game.GameSessionResponse;
import com.example.demo.dto.lobby.LobbyPlayerResponse;
import com.example.demo.dto.lobby.LobbyResponse;
import com.example.demo.dto.lobby.UpdateLobbySettingsRequest;
import com.example.demo.entity.*;
import com.example.demo.event.GameUpdatedEvent;
import com.example.demo.event.LobbyUpdatedEvent;
import com.example.demo.exception.*;
import com.example.demo.repository.GameSessionRepository;
import com.example.demo.repository.LobbyRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LobbyService {
    private final LobbyRepository lobbyRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final GameSessionRepository gameSessionRepository;
    private final GameSessionService gameSessionService;
    private final GameStateService gameStateService;
    private final DeckService deckService;
    private final InitialGameSetupService initialGameSetupService;

    @Transactional
    public LobbyResponse createLobby(CreateLobbyRequest request, Long userId) {
        User owner = getUserById(userId);
        if (!lobbyRepository.findByPlayerId(userId).isEmpty()) {
            throw new ConflictException("Пользователь уже находится в лобби");
        }

        Lobby lobby = new Lobby(request.getName(), owner, request.getMaxPlayers());

        Lobby savedLobby = lobbyRepository.save(lobby);
        LobbyResponse response = toLobbyResponse(savedLobby);

        eventPublisher.publishEvent(
                new LobbyUpdatedEvent(
                        "LOBBY_UPDATED",
                        savedLobby.getId(),
                        response
                )
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<LobbyResponse> getAvailableLobbies() {
        return lobbyRepository.findByStatus(LobbyStatus.WAITING)
                .stream()
                .map(this::toLobbyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LobbyResponse getLobbyById(Long lobbyId) {
        return toLobbyResponse(getLobbyEntityById(lobbyId));
    }

    @Transactional
    public LobbyResponse joinLobby(Long lobbyId, Long userId) {
        Lobby lobby = lobbyRepository.findByIdForUpdate(lobbyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Лобби с id " + lobbyId + " не найдено")
                );

        User user = getUserById(userId);

        if (lobby.getStatus() == LobbyStatus.STARTED) {
            throw new ConflictException("Игра в этом лобби уже началась");
        }

        boolean alreadyJoined = lobby.getPlayers()
                .stream()
                .anyMatch(player ->
                        player.getId().equals(userId)
                );

        if (alreadyJoined) {
            throw new ConflictException("Пользователь уже находится в лобби");
        }

        if (!lobbyRepository.findByPlayerId(userId).isEmpty()) {
            throw new ConflictException("Пользователь уже находится в другом лобби");
        }

        if (lobby.getPlayers().size() >= lobby.getMaxPlayers()) {
            throw new ConflictException("Лобби заполнено");
        }

        lobby.getPlayers().add(user);

        Lobby savedLobby = lobbyRepository.save(lobby);
        LobbyResponse response = toLobbyResponse(savedLobby);

        eventPublisher.publishEvent(
                new LobbyUpdatedEvent(
                        "LOBBY_UPDATED",
                        savedLobby.getId(),
                        response
                )
        );

        return response;
    }

    @Transactional
    public LobbyResponse leaveLobby(Long lobbyId, Long userId) {
        Lobby lobby = lobbyRepository.findByIdForUpdate(lobbyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Лобби с id " + lobbyId + " не найдено")
                );

        User player = lobby.getPlayers()
                .stream()
                .filter(user ->
                        user.getId().equals(userId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new ConflictException("Пользователь не находится в этом лобби")
                );

        boolean ownerLeaving = lobby.getOwner().getId().equals(userId);

        lobby.getPlayers().remove(player);

        if (lobby.getPlayers().isEmpty()) {
            eventPublisher.publishEvent(
                    new LobbyUpdatedEvent(
                            "LOBBY_DELETED",
                            lobby.getId(),
                            null
                    )
            );
            lobbyRepository.delete(lobby);

            return null;
        }

        if (ownerLeaving) {
            User newOwner = lobby.getPlayers().getFirst();
            lobby.setOwner(newOwner);
        }

        Lobby savedLobby = lobbyRepository.save(lobby);
        LobbyResponse response = toLobbyResponse(savedLobby);

        eventPublisher.publishEvent(
                new LobbyUpdatedEvent(
                        "LOBBY_UPDATED",
                        savedLobby.getId(),
                        response
                )
        );

        return response;
    }

    @Transactional
    public LobbyResponse updateLobbySettings(Long lobbyId, UpdateLobbySettingsRequest request, Long userId) {
        Lobby lobby = lobbyRepository.findByIdForUpdate(lobbyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Лобби с id " + lobbyId + " не найдено"));

        if (!lobby.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Только владелец лобби может изменять настройки");
        }

        if (lobby.getStatus() != LobbyStatus.WAITING) {
            throw new ConflictException("Нельзя изменять настройки после запуска игры");
        }

        if (request.getInitialTableCards() == null || request.getInitialTableCards() < 3
                || request.getInitialTableCards() > 10 || request.getInitialHandCards() == null
                || request.getInitialHandCards() < 3 || request.getInitialHandCards() > 10) {
            throw new IllegalArgumentException("Количество карт должно быть от 3 до 10");
        }

        lobby.setInitialTableCards(request.getInitialTableCards());
        lobby.setInitialHandCards(request.getInitialHandCards());

        Lobby savedLobby = lobbyRepository.save(lobby);
        LobbyResponse response = toLobbyResponse(savedLobby);

        eventPublisher.publishEvent(
                new LobbyUpdatedEvent(
                        "LOBBY_UPDATED",
                        savedLobby.getId(),
                        response
                )
        );

        return response;
    }

    @Transactional
    public LobbyResponse startGame(Long lobbyId, Long userId) {
        Lobby lobby = lobbyRepository.findByIdForUpdate(lobbyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Лобби с id " + lobbyId + " не найдено"));

        if (!lobby.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Только владелец лобби может начать игру");
        }

        if (lobby.getStatus() == LobbyStatus.STARTED) {
            throw new ConflictException("Игра уже запущена");
        }

        if (lobby.getPlayers().size() < 2) {
            throw new ConflictException("Для начала игры необходимо минимум 2 игрока");
        }

        GameSession gameSession = new GameSession();
        gameSession.setLobby(lobby);
        for (User user : lobby.getPlayers()) {
            gameSession.getPlayers().add(new GameSessionPlayer(gameSession, user));
        }
        gameSession.setStatus(GameSessionStatus.ACTIVE);
        gameSession.setStartedAt(LocalDateTime.now());

        GameSession savedGameSession = gameSessionRepository.save(gameSession);

        gameStateService.createInitialState(savedGameSession);
        deckService.createDeck(savedGameSession);
        initialGameSetupService.prepare(savedGameSession);

        lobby.setStatus(LobbyStatus.STARTED);
        Lobby savedLobby = lobbyRepository.save(lobby);

        GameSessionResponse gameResponse = gameSessionService.toResponse(savedGameSession);

        eventPublisher.publishEvent(
                new GameUpdatedEvent(
                        "GAME_STARTED",
                        savedGameSession.getId(),
                        gameResponse
                )
        );

        LobbyResponse response = toLobbyResponse(savedLobby);

        return response;
    }

    private Lobby getLobbyEntityById(Long lobbyId) {
        return lobbyRepository.findById(lobbyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Лобби с id " + lobbyId + " не найдено")
                );
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Пользователь с id " + userId + " не найден")
                );
    }

    private LobbyResponse toLobbyResponse(Lobby lobby) {
        List<LobbyPlayerResponse> players = lobby.getPlayers()
                .stream()
                .map(user -> new LobbyPlayerResponse(
                        user.getId(),
                        user.getName()
                ))
                .toList();

        return new LobbyResponse(
                lobby.getId(),
                lobby.getName(),
                lobby.getOwner().getId(),
                players,
                lobby.getMaxPlayers(),
                lobby.getInitialTableCards(),
                lobby.getInitialHandCards(),
                lobby.getStatus()
        );
    }
}