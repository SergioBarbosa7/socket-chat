package br.com.study.socketchat.client;

import br.com.study.socketchat.commons.Message;
import br.com.study.socketchat.commons.enums.MessageType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.net.Socket;
import java.net.ConnectException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cliente de chat com interface de linha de comando (CLI)
 * Conecta ao servidor e permite envio de mensagens e arquivos
 */
@SpringBootApplication
public class ChatClientApplication {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final String DOWNLOADS_DIRECTORY = "client_downloads/";

    private Socket socket;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String username;
    private boolean isConnected = false;
    private ExecutorService executor;
    private Scanner scanner;

    public ChatClientApplication() {
        this.executor = Executors.newSingleThreadExecutor();
        this.scanner = new Scanner(System.in);
        createDownloadsDirectory();
    }

    private void createDownloadsDirectory() {
        File dir = new File(DOWNLOADS_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void start() {
        printWelcome();

        // Solicitar nome de usuário
        System.out.print("Digite seu nome de usuário: ");
        username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("Nome de usuário não pode estar vazio!");
            return;
        }

        // Conectar ao servidor
        if (!connectToServer()) {
            return;
        }

        // Iniciar thread para receber mensagens
        executor.execute(this::receiveMessages);

        // Loop principal da interface
        runInterface();
    }

    private void receiveMessages() {
        try {
            while (isConnected) {
                Message message = (Message) inputStream.readObject();
                handleReceivedMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (isConnected) {
                System.out.println("\n Conexão perdida com o servidor");
                isConnected = false;
            }
        }
    }

    private void printWelcome() {
        System.out.println("==============================================================");
        System.out.println("/                      SOCKET WHATSAPP                       /");
        System.out.println("/                      Chat Distribuído                      /");
        System.out.println("==============================================================");
        System.out.println();
    }

    private boolean connectToServer() {
        try {
            System.out.println("Conectando ao servidor " + SERVER_HOST + ":" + SERVER_PORT + "...");

            socket = new Socket(SERVER_HOST, SERVER_PORT);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            // Enviar tentativa de login
            Message loginMessage = new Message(MessageType.LOGIN, username, "SERVER", username);
            outputStream.writeObject(loginMessage);
            outputStream.flush();

            // Aguardar resposta do servidor
            Message response = (Message) inputStream.readObject();

            if (response.getType() == MessageType.LOGIN_SUCCESS) {
                isConnected = true;
                System.out.println("Conectado com sucesso como: " + username);
                return true;
            } else {
                System.out.println("Falha no login: " + response.getContent());
                return false;
            }

        } catch (ConnectException e) {
            System.out.println("Não foi possível conectar ao servidor. Verifique se o servidor está rodando.");
            return false;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Erro ao conectar: " + e.getMessage());
            return false;
        }
    }

    private void runInterface() {
        printHelp();

        while (isConnected) {
            System.out.print("\n" + username + "> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            processCommand(input);
        }
    }

    private void processCommand(String input) {
        String[] parts = input.split(" ", 3);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/help":
            case "/h":
                printHelp();
                break;
            case "/msg":
            case "/group":
            case "/create":
            case "/join":
            case "/leave":
            case "/file":
            case "/users":
            case "/groups":
            case "/quit":
            case "/exit":
                disconnect();
                break;

            default:
                System.out.println("Comando não reconhecido. Digite /help para ver os comandos disponíveis.");
        }
    }

    private void printHelp() {
        System.out.println("\nCOMANDOS DISPONÍVEIS:");
        System.out.println("---------------------------------------------------------------");
        System.out.println("/msg <usuário> <mensagem>     - Enviar mensagem privada");
        System.out.println("/group <grupo> <mensagem>     - Enviar mensagem para grupo");
        System.out.println("/create <nome_grupo>          - Criar novo grupo");
        System.out.println("/join <nome_grupo>            - Entrar em um grupo");
        System.out.println("/leave <nome_grupo>           - Sair de um grupo");
        System.out.println("/file <destino> <arquivo>     - Enviar arquivo");
        System.out.println("/users                        - Listar usuários online");
        System.out.println("/groups                       - Listar grupos disponíveis");
        System.out.println("/help                         - Mostrar esta ajuda");
        System.out.println("/quit                         - Sair do chat");
        System.out.println("---------------------------------------------------------------");
        System.out.println("Dica: Para grupos, use # antes do nome (ex: /group #geral mensagem)");
    }

    private void handleReceivedMessage(Message message) {
        switch (message.getType()) {
            case PRIVATE_MESSAGE:
            case GROUP_MESSAGE:
            case FILE_MESSAGE:
            case USERS_LIST:
            case GROUPS_LIST:
            case GROUP_CREATED:
            case GROUP_JOINED:
            case GROUP_LEFT:
            case GROUP_CREATE_FAILED:
            case GROUP_JOIN_FAILED:
            case GROUP_LEAVE_FAILED:
            default:
                System.out.println("\n[SERVIDOR] " + message.getContent());
        }

        // Reexibir prompt
        System.out.print(username + "> ");
    }

    private void disconnect() {
        try {
            if (isConnected) {
                Message disconnectMessage = new Message(MessageType.DISCONNECT, username, "SERVER", "");
                outputStream.writeObject(disconnectMessage);
                outputStream.flush();
            }
        } catch (IOException e) {
            // Ignorar erros na desconexão
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        isConnected = false;

        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            // Ignorar erros na limpeza
        }

        executor.shutdown();
        System.out.println("\nDesconectado do servidor. Até logo!");
        System.exit(0);
    }

    public static void main(String[] args) {
        ChatClientApplication client = new ChatClientApplication();

        Runtime.getRuntime().addShutdownHook(new Thread(client::cleanup));

        client.start();
    }
}
