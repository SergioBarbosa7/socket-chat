package br.com.study.socketchat.commons;

import br.com.study.socketchat.commons.enums.MessageType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Classe que representa uma mensagem no sistema de chat
 * Implementa Serializable para permitir transmissão via socket
 */
@Data
@NoArgsConstructor
public class Message implements Serializable {
    private MessageType type;
    private String from;
    private String to;
    private String content;
    private LocalDateTime timestamp;
    private String fileName;

    public Message(MessageType type, String from, String to, String content) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public Message(MessageType type, String sender, String recipient, byte[] fileData, String fileName) {
        this.type = type;
        this.from = sender;
        this.to = recipient;
        this.content = Base64.getEncoder().encodeToString(fileData);
        this.fileName = fileName;
        this.timestamp = LocalDateTime.now();
    }

    public byte[] getFileData() {
        return Base64.getDecoder().decode(content);
    }

}
