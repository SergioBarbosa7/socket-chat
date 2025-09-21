package br.com.study.socketchat.server;

import br.com.study.socketchat.commons.Group;
import br.com.study.socketchat.commons.Message;
import br.com.study.socketchat.commons.User;
import br.com.study.socketchat.commons.enums.MessageType;
import br.com.study.socketchat.server.group.service.GroupService;
import br.com.study.socketchat.server.service.ChatService;
import br.com.study.socketchat.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

/**
 * Handler para gerenciar a comunicação com um cliente específico
 * Cada cliente conectado tem sua própria thread com este handler
 */
public class ChatHandler implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ChatHandler.class);
    private static final String SERVER_USER = "SERVER";

    private final Socket clientSocket;
    private final SessionManager sessionManager;
    private final ChatService chatService;
    private final GroupService groupService;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String username;
    private boolean isConnected = true;

    public ChatHandler(Socket clientSocket, SessionManager sessionManager, ChatService chatService, GroupService groupService) {
        this.clientSocket = clientSocket;
        this.sessionManager = sessionManager;
        this.chatService = chatService;
        this.groupService = groupService;
    }

    @Override
    public void run() {
        try {
            outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
            inputStream = new ObjectInputStream(clientSocket.getInputStream());

            if (!authenticate()) {
                return;
            }

            LOG.info("Cliente autenticado: {}", username);

            while (isConnected) {
                try {
                    Message message = (Message) inputStream.readObject();
                    handleMessage(message);
                } catch (SocketException e) {
                    LOG.info("Cliente desconectado: {}", username);
                    break;
                } catch (EOFException e) {
                    LOG.info("Conexão encerrada pelo cliente: {}", username);
                    break;
                } catch (ClassNotFoundException e) {
                    LOG.error("Erro ao deserializar mensagem", e);
                }
            }

        } catch (IOException e) {
            LOG.error("Erro na comunicação com cliente", e);
        } finally {
            cleanup();
        }
    }

    private boolean authenticate() {
        try {
            Message message = (Message) inputStream.readObject();
            username = message.getFrom();
            if (message.getType() == MessageType.LOGIN) {
                sessionManager.registerUser(message.getContent(), this);

                Message sucess = new Message(MessageType.LOGIN_SUCCESS, SERVER_USER, username, "Login succeeded");
                sendGenericMessage(sucess);
                chatService.deliverOfflineMessages(username);
            } else {
                sendGenericMessage(buildErrorMessage(MessageType.LOGIN_FAILED, "INVALID MESSAGE TYPE, FIRST MESSAGE TYPE SHOULD BE LOGIN"));
                return false;
            }
        } catch (IllegalArgumentException iae) {
            sendGenericMessage(buildErrorMessage(MessageType.LOGIN_FAILED, iae.getMessage()));
            return false;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case PRIVATE_MESSAGE:
                sendPrivateMessage(message);
                break;
            case REQUEST_USERS_LIST:
                listUsers();
                break;
            case CREATE_GROUP:
                createGroup(message);
                break;
            case GROUP_MESSAGE:
                groupMessage(message);
                break;
            case JOIN_GROUP:
                joinGroup(message);
                break;
            case LEAVE_GROUP:
                leaveGroup(message);
                break;
            case FILE_MESSAGE:
            case REQUEST_GROUPS_LIST:
                listGroups();
                break;
            case DISCONNECT:
                isConnected = false;
                break;

            default:
                LOG.warn("Tipo de mensagem não reconhecido: {}", message.getType());
        }
    }

    private void groupMessage(Message message) {
        try {
            chatService.sendGroupMessage(message, this);
        } catch (IllegalArgumentException iae) {
            sendGenericMessage(buildErrorMessage(MessageType.GROUP_CREATE_FAILED, iae.getMessage()));
        }
    }

    private void listGroups() {
        List<Group> groups = groupService.findGroups();
        if (groups.isEmpty()) {
            sendGenericMessage(new Message(MessageType.GROUPS_LIST, SERVER_USER, username, "No groups available"));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Available groups:\n");
        for (Group group : groups) {
            stringBuilder.append(group.toString()).append("\n");
        }
        sendGenericMessage(new Message(MessageType.USERS_LIST, SERVER_USER, username, stringBuilder.toString()));
    }

    private void createGroup(Message message) {
        try {
            Group group = new Group(message.getContent(), message.getFrom());
            groupService.createGroup(group);
            String msg = "Group " + group.getName() + " created";
            LOG.info(msg);
            sendGenericMessage(new Message(MessageType.GROUP_CREATED, SERVER_USER, username, msg));
        } catch (IllegalArgumentException iae) {
            sendGenericMessage(buildErrorMessage(MessageType.GROUP_CREATE_FAILED, iae.getMessage()));
        }

    }

    private void leaveGroup(Message message) {
        try {
            groupService.leaveGroup(username, message.getContent());
            String msg = "User " + username + " left group " + message.getContent();
            LOG.info(msg);
            sendGenericMessage(new Message(MessageType.GROUP_LEFT, SERVER_USER, username, msg));
        } catch (IllegalArgumentException iae) {
            sendGenericMessage(buildErrorMessage(MessageType.GROUP_LEAVE_FAILED, iae.getMessage()));
        }
    }

    private void joinGroup(Message message) {
        try {
            groupService.joinGroup(username, message.getContent());
            String msg = "User " + username + " joined group " + message.getContent();
            LOG.info(msg);
            sendGenericMessage(new Message(MessageType.GROUP_JOINED, SERVER_USER, username, msg));
        } catch (IllegalArgumentException iae) {
            sendGenericMessage(buildErrorMessage(MessageType.GROUP_JOIN_FAILED, iae.getMessage()));
        }
    }

    private void listUsers() {
        List<User> users = sessionManager.findUsers();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Available users:\n");
        for (User user : users) {
            stringBuilder.append(user.toString()).append("\n");
        }
        sendGenericMessage(new Message(MessageType.USERS_LIST, SERVER_USER, username, stringBuilder.toString()));
    }

    private void sendPrivateMessage(Message message) {
        try {
            chatService.sendPrivateMessage(message, this);
        } catch (IllegalArgumentException iae) {
            sendGenericMessage(buildErrorMessage(MessageType.ERROR_MESSAGE, iae.getMessage()));
        }

    }

    private void cleanup() {
        try {
            if (username != null) {
                sessionManager.unregisterUser(username);
            }

            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }

        } catch (IOException e) {
            LOG.error("Erro durante limpeza", e);
        }
    }

    private Message buildErrorMessage(MessageType messageType, String error) {
        return new Message(messageType, "SERVER", username, error);
    }

    public void sendGenericMessage(Message message) {
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            LOG.error("Erro ao enviar mensagem genérica para {}", username, e);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
