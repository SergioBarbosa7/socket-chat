package br.com.study.socketchat.commons.enums;

/**
 * Enumeração que define os tipos de mensagens suportados pelo sistema
 */
public enum MessageType {
    // Autenticação
    LOGIN,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    DISCONNECT,

    // Mensagens de texto
    PRIVATE_MESSAGE,
    GROUP_MESSAGE,

    // Gerenciamento de grupos
    CREATE_GROUP,
    GROUP_CREATED,
    GROUP_CREATE_FAILED,
    JOIN_GROUP,
    GROUP_JOINED,
    GROUP_JOIN_FAILED,
    LEAVE_GROUP,
    GROUP_LEFT,
    GROUP_LEAVE_FAILED,

    // Arquivos
    FILE_MESSAGE,
    FILE_GROUP,
    FILE_RECEIVED,

    // Informações do servidor
    USERS_LIST,
    GROUPS_LIST,
    REQUEST_USERS_LIST,
    REQUEST_GROUPS_LIST,

    // Sistema
    SERVER_MESSAGE,
    ERROR_MESSAGE,
    HEARTBEAT
}

