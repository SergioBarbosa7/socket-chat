package br.com.study.socketchat.server.storage;

import br.com.study.socketchat.commons.Message;

import java.util.List;

public interface OfflineMessageStorage {
    void storeMessage(String username, Message message);
    List<Message> retrieveMessages(String username);
    boolean hasMessages(String username);
}
