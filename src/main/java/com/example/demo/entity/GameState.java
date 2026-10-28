package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_states")
@Getter
@Setter
@NoArgsConstructor
public class GameState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "game_session_id",
            nullable = false,
            unique = true
    )
    private GameSession gameSession;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private long revision = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_player_id")
    private User currentPlayer;

    @Column(nullable = false)
    private long turnNumber = 1;
}
