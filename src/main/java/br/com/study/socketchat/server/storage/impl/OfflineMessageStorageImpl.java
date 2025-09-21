package br.com.study.socketchat.server.storage.impl;

import br.com.study.socketchat.commons.Message;
import br.com.study.socketchat.server.storage.OfflineMessageStorage;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OfflineMessageStorageImpl implements OfflineMessageStorage {
    private final Map<String, List<Message>> offlineMessages;

    public OfflineMessageStorageImpl() {
        this.offlineMessages = new HashMap<>();
    }

    @Override
    public void storeMessage(String username, Message message) {
        offlineMessages
                .computeIfAbsent(username, k -> new ArrayList<>())
                .add(message);
    }

    @Override
    public List<Message> retrieveMessages(String username) {
        List<Message> messages = offlineMessages.getOrDefault(username, Collections.emptyList());
        offlineMessages.remove(username); // limpa após entregar
        return messages;
    }

    @Override
    public boolean hasMessages(String username) {
        return offlineMessages.containsKey(username);
    }
}
