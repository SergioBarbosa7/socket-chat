package br.com.study.socketchat.server;


import br.com.study.socketchat.server.group.GroupManager;
import br.com.study.socketchat.server.group.service.GroupService;
import br.com.study.socketchat.server.service.ChatService;
import br.com.study.socketchat.server.session.SessionManager;
import br.com.study.socketchat.server.storage.OfflineMessageStorage;
import br.com.study.socketchat.server.storage.impl.OfflineMessageStorageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor de chat distribuído que gerencia conexões de múltiplos clientes
 * Suporta mensagens privadas, grupos e envio de arquivos
 */
@SpringBootApplication
public class SocketServerChatApplication {

    private static final int PORT = 12345;
    private static final String FILES_DIRECTORY = "server_files/";

    private ServerSocket serverSocket;
    private boolean isRunning = false;
    private ExecutorService threadPool;
    
    private final OfflineMessageStorage offlineMessageStorage;
    private final SessionManager sessionManager;
    private final ChatService chatService;
    private final GroupManager groupManager;
    private final GroupService groupService;


    public SocketServerChatApplication() {
        this.threadPool = Executors.newCachedThreadPool();
        this.sessionManager = new SessionManager();
        this.offlineMessageStorage = new OfflineMessageStorageImpl();
        this.groupManager =  new GroupManager();
        this.groupService = new GroupService(groupManager);
        this.chatService = new ChatService(sessionManager, groupService, offlineMessageStorage);

        createFilesDirectory();
    }
    
    private void createFilesDirectory() {
        File dir = new File(FILES_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            System.out.println("Servidor iniciado na porta " + PORT);
            System.out.println("Diretório de arquivos: " + FILES_DIRECTORY);
            
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("🔗 Nova conexão recebida: " + clientSocket.getInetAddress());

                    ChatHandler clientHandler = new ChatHandler(clientSocket, sessionManager, chatService, groupService);
                    threadPool.execute(clientHandler);
                    
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
        }
    }
    
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            threadPool.shutdown();
            System.out.println("Servidor parado");
        } catch (IOException e) {
            System.err.println("Erro ao parar servidor: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SocketServerChatApplication server = new SocketServerChatApplication();
        
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        server.start();
    }
}

