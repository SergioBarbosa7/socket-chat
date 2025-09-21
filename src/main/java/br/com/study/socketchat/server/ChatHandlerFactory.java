package br.com.study.socketchat.server;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.net.Socket;

@Component
public class ChatHandlerFactory {

    private final ObjectProvider<ChatHandler> chatHandlerProvider;

    public ChatHandlerFactory(ObjectProvider<ChatHandler> chatHandlerProvider) {
        this.chatHandlerProvider = chatHandlerProvider;
    }

    public ChatHandler create(Socket socket) {
        return chatHandlerProvider.getObject().initialize(socket);
    }
}
