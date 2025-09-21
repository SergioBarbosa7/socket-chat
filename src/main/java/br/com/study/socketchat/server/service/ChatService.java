package br.com.study.socketchat.server.service;

import br.com.study.socketchat.commons.Group;
import br.com.study.socketchat.commons.Message;
import br.com.study.socketchat.commons.enums.MessageType;
import br.com.study.socketchat.server.ChatHandler;
import br.com.study.socketchat.server.group.service.GroupService;
import br.com.study.socketchat.server.storage.OfflineMessageStorage;
import br.com.study.socketchat.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Serviço responsável pela lógica de envio de mensagens.
 * Aplica regras de negócio do chat (online/offline, entrega, erro).
 */
@Service
public class ChatService {
    private static final Logger LOG = LoggerFactory.getLogger(ChatService.class);
    private static final String SERVER_USER = "SERVER";

    private final SessionManager sessionManager;
    private final GroupService groupService;
    private final OfflineMessageStorage offlineMessageStore;

    public ChatService(SessionManager sessionManager, GroupService groupService, OfflineMessageStorage offlineMessageStore) {
        this.sessionManager = sessionManager;
        this.groupService = groupService;
        this.offlineMessageStore = offlineMessageStore;
    }

    /**
     * Envia mensagem privada.
     */
    public void sendPrivateMessage(Message message, ChatHandler sender) {
        deliverToUser(message.getTo(), message, sender);
    }

    /**
     * Envia mensagem em grupo.
     */
    public void sendGroupMessage(Message message, ChatHandler sender) {
        String groupName = message.getTo();
        Group group = groupService.findGroupWithUser(message.getFrom(), groupName);

        if (group == null) {
            sender.sendGenericMessage(buildErrorMessage(message, "Grupo não existe: " + groupName));
            return;
        }

        Set<String> members = group.getMembers();
        if (!members.contains(message.getFrom())) {
            sender.sendGenericMessage(buildErrorMessage(message, "Você não é membro do grupo: " + groupName));
            return;
        }

        String messageSender = message.getFrom();
        // Identifica que a mensagem vem de um grupo
        message.setFrom(message.getFrom() + "@" + group.getName());

        for (String member : members) {
            if (!member.equals(messageSender)) {
                deliverToUser(member, message, sender);
            }
        }
    }

    /**
     * Lida com a entrega da mensagem para um usuário (privado ou grupo).
     */
    private void deliverToUser(String receiver, Message message, ChatHandler sender) {
        // Valida se usuário existe
        if (!sessionManager.isUserNameRegistered(receiver)) {
            sender.sendGenericMessage(buildErrorMessage(message, "Usuário não registrado: " + receiver));
            return;
        }

        // Se offline, guarda para entrega futura
        if (!sessionManager.isUserOnline(receiver)) {
            LOG.info("Usuário {} está offline. Armazenando mensagem offline.", receiver);
            offlineMessageStore.storeMessage(receiver, message);
            return;
        }

        // Tenta enviar
        ChatHandler receiverHandler = sessionManager.getHandler(receiver);
        if (receiverHandler != null) {
            receiverHandler.sendGenericMessage(message);
        } else {
            sender.sendGenericMessage(buildErrorMessage(message, "Erro ao entregar mensagem para " + receiver));
        }
    }

    /**
     * Reenvia mensagens armazenadas para usuário logado.
     */
    public void deliverOfflineMessages(String username) {
        var messages = offlineMessageStore.retrieveMessages(username);
        if (messages.isEmpty()) return;

        LOG.info("Entregando {} mensagens offline para {}", messages.size(), username);
        ChatHandler handler = sessionManager.getHandler(username);
        if (handler != null) {
            for (Message msg : messages) {
                handler.sendGenericMessage(msg);
            }
        }
    }

    private Message buildErrorMessage(Message original, String error) {
        return new Message(MessageType.ERROR_MESSAGE, SERVER_USER, original.getFrom(), error);
    }
}
