package br.com.study.socketchat.commons;

import lombok.Data;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Classe que representa um usuário do sistema de chat
 */
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private LocalDateTime lastSeen;
    private boolean isOnline;

    public User(String username) {
        this.username = username;
        this.lastSeen = LocalDateTime.now();
        this.isOnline = true;
    }

    public void setOnline(boolean online) {
        this.isOnline = online;
        if (!online) {
            this.lastSeen = LocalDateTime.now();
        }
    }

    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return username + (isOnline ? " (online)" : " (offline) - Last seen: " + lastSeen);
    }
}

