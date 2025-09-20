package br.com.study.socketchat.commons;

import br.com.study.socketchat.commons.enums.MessageType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Classe que representa uma mensagem no sistema de chat
 * Implementa Serializable para permitir transmissão via socket
 */
@Data
public class Message implements Serializable {
    private MessageType type;
    private String from;
    private String to;
    private String content;
    private LocalDateTime timestamp;

    private String fileName;
    private byte[] fileData;

    public Message(MessageType type, String from, String to, String content) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
}
