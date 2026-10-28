package com.example.demo.controller;

import com.example.demo.dto.game.CardDrawResponse;
import com.example.demo.dto.game.GameSessionResponse;
import com.example.demo.dto.game.GameStateResponse;
import com.example.demo.dto.game.TurnResponse;
import com.example.demo.entity.GameSessionStatus;
import com.example.demo.service.CardDrawService;
import com.example.demo.service.GameSessionService;
import com.example.demo.service.GameStateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class GameSessionController {
    private final GameSessionService gameSessionService;
    private final GameStateService gameStateService;
    private final CardDrawService cardDrawService;

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить игровую сессию",
            description = "Возвращает информацию об игровой сессии и её игроках"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Игровая сессия найдена"),
            @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не является участником игры"),
            @ApiResponse(responseCode = "404", description = "Игровая сессия не найдена")
    })
    public ResponseEntity<GameSessionResponse> getGameSession(
            @Parameter(description = "ID игровой сессии", example = "1")
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(gameSessionService.getGameSessionById(id, userId));
    }

    @GetMapping("/{id}/state")
    @Operation(
            summary = "Получить состояние игры",
            description = "Возвращает состояние игры только её участнику"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Состояние игры получено"),
            @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не является участником игры"),
            @ApiResponse(responseCode = "404", description = "Игра или её состояние не найдены")
    })
    public ResponseEntity<GameStateResponse> getGameState(
            @Parameter(description = "ID игровой сессии", example = "1")
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(gameStateService.getState(id, userId));
    }

    @PatchMapping("/{id}/finish")
    @Operation(
            summary = "Завершить игровую сессию",
            description = "Завершает игровую сессию и отправляет GAME_FINISHED"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Игра завершена"),
            @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не является участником игры"),
            @ApiResponse(responseCode = "404", description = "Игровая сессия не найдена"),
            @ApiResponse(responseCode = "409", description = "Игра уже завершена")
    })
    public ResponseEntity<GameSessionResponse> finishGame(
            @Parameter(description = "ID игровой сессии", example = "1")
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(gameSessionService.finishGame(id, userId));
    }

    @PostMapping("/{id}/cards/draw")
    @Operation(
            summary = "Получить карту из колоды",
            description = "Выдаёт авторизованному игроку случайную карту из колоды его игровой сессии"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Карта успешно выдана"),
            @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не является участником игры"),
            @ApiResponse(responseCode = "404", description = "Игра или колода не найдены"),
            @ApiResponse(responseCode = "409", description = "В колоде не осталось карт")
    })
    public ResponseEntity<CardDrawResponse> drawCard(
            @Parameter(description = "ID игровой сессии", example = "1")
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(cardDrawService.drawCard(id, userId));
    }

    @GetMapping("/{id}/turn")
    @Operation(summary = "Получить текущий ход")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Текущий ход получен"),
            @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не является участником игры"),
            @ApiResponse(responseCode = "404", description = "Игра или состояние игры не найдены")
    })
    public ResponseEntity<TurnResponse> getTurn(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(gameStateService.getTurn(id, userId));
    }

    @PostMapping("/{id}/turn/end")
    @Operation(summary = "Завершить текущий ход и передать ход следующему игроку")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ход завершён, передан следующему игроку"),
            @ApiResponse(responseCode = "401", description = "Требуется авторизация"),
            @ApiResponse(responseCode = "403", description = "Пользователь не является участником игры"),
            @ApiResponse(responseCode = "404", description = "Игра или состояние игры не найдены"),
            @ApiResponse(responseCode = "409", description = "Игра не запущена или сейчас не ваш ход")
    })
    public ResponseEntity<TurnResponse> endTurn(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(gameStateService.endTurn(id, userId));
    }

}