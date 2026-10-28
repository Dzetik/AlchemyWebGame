package com.example.demo.dto.game;

public record TurnResponse(
        Long gameId,
        Long currentPlayerId,
        long turnNumber
) {}
