package br.com.study.socketchat.server.session;

import br.com.study.socketchat.commons.User;
import br.com.study.socketchat.server.ChatHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SessionManager {
    private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);


    private Map<String, ChatHandler> sessions;
    private Map<String, User> registeredUsers;

    public SessionManager() {
        sessions = new HashMap<>();
        registeredUsers = new HashMap<>();
    }

    public void registerUser(String userName, ChatHandler handler) {
        if (sessions.containsKey(userName)) {

            throw new IllegalArgumentException("Usuário já está conectado: " + userName);
        }
        if (!isUserNameRegistered(userName)) {
            addNewUser(userName);
        } else {
            updateUser(userName);
        }
        sessions.put(userName, handler);
    }


    private void updateUser(String userName) {
        registeredUsers.get(userName).setOnline(true);
    }

    private void addNewUser(String userName) {
        registeredUsers.put(userName, new User(userName));
    }


    public boolean isUserNameRegistered(String userName) {
        return registeredUsers.containsKey(userName);
    }

    public void unregisterUser(String username) {
        sessions.remove(username);
        registeredUsers.get(username).setOnline(false);

        LOG.info("Usuário deslogado com sucesso: {}", username);
    }

    public boolean isUserOnline(String isOnline) {
        return registeredUsers.get(isOnline).isOnline();
    }

    public ChatHandler getHandler(String username) {
        return sessions.get(username);
    }

    public List<User> findUsers() {
        return new ArrayList<>(registeredUsers.values());
    }
}
