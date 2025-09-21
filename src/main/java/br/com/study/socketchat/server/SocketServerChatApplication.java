package br.com.study.socketchat.server;

import br.com.study.socketchat.server.group.service.GroupService;
import br.com.study.socketchat.server.service.ChatService;
import br.com.study.socketchat.server.session.SessionManager;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servidor de chat distribuído que gerencia conexões de múltiplos clientes
 * Suporta mensagens privadas, grupos e envio de arquivos
 */
@SpringBootApplication
public class SocketServerChatApplication implements CommandLineRunner {

    private static final int PORT = 12345;
    private static final String FILES_DIRECTORY = "server_files/";

    private static final Logger LOG = LoggerFactory.getLogger(SocketServerChatApplication.class);

    private final SessionManager sessionManager;
    private final ChatService chatService;
    private final GroupService groupService;
    private final ChatHandlerFactory chatHandlerFactory;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private ServerSocket serverSocket;
    private boolean isRunning = false;

    public SocketServerChatApplication(SessionManager sessionManager,
                                       ChatService chatService,
                                       GroupService groupService,
                                       ChatHandlerFactory chatHandlerFactory) {
        this.sessionManager = sessionManager;
        this.chatService = chatService;
        this.groupService = groupService;
        this.chatHandlerFactory = chatHandlerFactory;
        createFilesDirectory();
    }

    private void createFilesDirectory() {
        File dir = new File(FILES_DIRECTORY);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                LOG.info("Diretório de arquivos criado em {}", dir.getAbsolutePath());
            }
        }
    }

    @Override
    public void run(String... args) {
        LOG.info("Dependências carregadas: SessionManager={}, GroupService={}, ChatService={}",
                sessionManager.getClass().getSimpleName(),
                groupService.getClass().getSimpleName(),
                chatService.getClass().getSimpleName());
        start();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            isRunning = true;
            LOG.info("Servidor iniciado na porta {}", PORT);
            LOG.info("Diretório de arquivos: {}", FILES_DIRECTORY);

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    LOG.info("🔗 Nova conexão recebida: {}", clientSocket.getInetAddress());

                    ChatHandler clientHandler = chatHandlerFactory.create(clientSocket);
                    threadPool.execute(clientHandler);

                } catch (IOException e) {
                    if (isRunning) {
                        LOG.error("Erro ao aceitar conexão", e);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Erro ao iniciar servidor", e);
        }
    }

    @PreDestroy
    public void onDestroy() {
        stop();
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            LOG.info("Servidor parado");
        } catch (IOException e) {
            LOG.error("Erro ao parar servidor", e);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(SocketServerChatApplication.class, args);
    }
}
