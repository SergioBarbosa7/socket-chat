package br.com.study.socketchat.client;

import br.com.study.socketchat.commons.Message;
import br.com.study.socketchat.commons.enums.MessageType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cliente de chat com interface de linha de comando (CLI)
 * Conecta ao servidor e permite envio de mensagens e arquivos
 */
@SpringBootApplication
public class ChatClientApplication {
    private static final String SERVER_HOST = resolveServerHost();
    private static final int SERVER_PORT = resolveServerPort();
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

    private static String resolveServerHost() {
        String env = System.getenv("CHAT_SERVER_HOST");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String property = System.getProperty("chat.server.host");
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        return "localhost";
    }

    private static int resolveServerPort() {
        String env = System.getenv("CHAT_SERVER_PORT");
        if (env != null && !env.isBlank()) {
            try {
                return Integer.parseInt(env.trim());
            } catch (NumberFormatException ex) {
                System.out.println("CHAT_SERVER_PORT inválida: " + env + ". Usando porta padrão 12345.");
            }
        }

        String property = System.getProperty("chat.server.port");
        if (property != null && !property.isBlank()) {
            try {
                return Integer.parseInt(property.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Propriedade chat.server.port inválida: " + property + ". Usando porta padrão 12345.");
            }
        }

        return 12345;
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
                System.out.println("\n Conexão perdida com o servidor" + e.getMessage());
                e.printStackTrace();
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
            case "/quit":
            case "/exit":
                disconnect();
                break;
            case "/help":
            case "/h":
                printHelp();
                break;
            case "/msg":
                if (parts.length == 3) {
                    sendPrivateMessage(parts[1], parts[2]);
                } else {
                    System.out.println("ERR: Uso Indevido, correto: /msg <usuário> <mensagem>");
                }
                break;
            case "/users":
                listUsers();
                break;
            case "/group":
                if (parts.length == 3) {
                    sendGroupMessage(parts[1], parts[2]);
                } else {
                    System.out.println("ERR: Uso Indevido, correto: /group <grupo> <mensagem>");
                }
                break;
            case "/create":
                if (parts.length == 2) {
                    createGroup(parts[1]);
                } else {
                    System.out.println("ERR: Uso Indevido, correto: /create <grupo>");
                }
                break;
            case "/join":
                if (parts.length == 2) {
                    joinGroup(parts[1]);
                } else {
                    System.out.println("ERR: Uso Indevido, correto: /join <grupo>");
                }
                break;
            case "/leave":
                if (parts.length == 2) {
                    leaveGroup(parts[1]);
                } else {
                    System.out.println("ERR: Uso Indevido, correto: /join <grupo>");
                }
                break;
            case "/file":
                if (parts.length == 3) {
                    sendFile(parts[1], parts[2]);
                } else {
                    System.out.println("ERR: Uso Indevido, correto: /file <destino> <arquivo>");
                }
                break;
            case "/groups":
                listGroups();
                break;
            default:
                System.out.println("Comando não reconhecido. Digite /help para ver os comandos disponíveis.");
        }
    }

    private void sendGroupMessage(String groupName, String content) {
        try {
            Message message = new Message(MessageType.GROUP_MESSAGE, username, groupName, content);
            sendGenericMessage(message);
        } catch (IOException e) {
            System.out.println("Erro ao criar grupo: " + e.getMessage());
        }
    }

    private void leaveGroup(String groupName) {
        try {
            Message message = new Message(MessageType.LEAVE_GROUP, username, null, groupName);
            sendGenericMessage(message);
        } catch (IOException e) {
            System.out.println("Erro ao criar grupo: " + e.getMessage());
        }
    }

    private void listGroups() {
        try {
            Message message = new Message(MessageType.REQUEST_GROUPS_LIST, username, null, null);
            sendGenericMessage(message);
        } catch (IOException e) {
            System.out.println("Erro ao requisitar usuarios: " + e.getMessage());
        }
    }


    private void createGroup(String groupName) {
        try {
            Message message = new Message(MessageType.CREATE_GROUP, username, null, groupName);
            sendGenericMessage(message);
        } catch (IOException e) {
            System.out.println("Erro ao criar grupo: " + e.getMessage());
        }
    }

    private void joinGroup(String groupName) {
        try {
            Message message = new Message(MessageType.JOIN_GROUP, username, null, groupName);
            sendGenericMessage(message);
        } catch (IOException e) {
            System.out.println("Erro ao entrar em grupo: " + e.getMessage());
        }
    }

    private void listUsers() {
        try {
            Message message = new Message(MessageType.REQUEST_USERS_LIST, username, null, null);
            sendGenericMessage(message);
        } catch (IOException e) {
            System.out.println("Erro ao requisitar usuarios: " + e.getMessage());
        }
    }

    private void sendPrivateMessage(String to, String content) {
        try {
            Message message = new Message(MessageType.PRIVATE_MESSAGE, username, to, content);
            sendGenericMessage(message);
        } catch (IOException e) {
            System.out.println("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    private void sendFile(String destination, String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("Arquivo não encontrado: " + filePath);
            return;
        }

        String target = destination;
        MessageType messageType = MessageType.FILE_MESSAGE;

        if (destination.startsWith("#")) {
            target = destination.substring(1);
            if (target.isBlank()) {
                System.out.println("ERR: Nome de grupo inválido.");
                return;
            }
            messageType = MessageType.FILE_GROUP;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            Message message = new Message(messageType, username, target, fileBytes, file.getName());
            sendGenericMessage(message);
            System.out.println("Arquivo \"" + file.getName() + "\" enviado para " + destination + ".");
        } catch (IOException e) {
            System.out.println("Erro ao enviar arquivo: " + e.getMessage());
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
                printReceivedMessage(message);
                break;
            case GROUP_CREATED:
            case GROUP_JOINED:
            case GROUP_LEFT:
            case GROUPS_LIST:
            case USERS_LIST:
                printGenericMessage(message);
                break;
            case FILE_MESSAGE:
            case FILE_GROUP:
                handleIncomingFile(message);
                break;
            case FILE_RECEIVED:
                printGenericMessage(message);
                break;
            case GROUP_LEAVE_FAILED:
            case GROUP_JOIN_FAILED:
            case GROUP_CREATE_FAILED:
            case ERROR_MESSAGE:
                printErrorMessage(message);
                break;
            default:
                System.out.println("\n[SERVIDOR] " + message.getContent());
        }

        // Reexibir prompt
        System.out.print(username + "> ");
    }

    private void printGenericMessage(Message message) {
        System.out.println(message.getContent());
    }

    private void printReceivedMessage(Message message) {
        System.out.println(message.getFrom() + "(" + message.getTimestamp() + "): " + message.getContent());
    }

    private void printErrorMessage(Message message) {
        System.out.println("ERROR(" + message.getType() +"): " + message.getContent());
    }

    public void sendGenericMessage(Message message) throws IOException {
        outputStream.writeObject(message);
        outputStream.flush();
    }

    private void handleIncomingFile(Message message) {
        String sender = message.getFrom();
        String originalFileName = message.getFileName();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "arquivo_recebido";
        }

        byte[] fileData;
        try {
            fileData = message.getFileData();
        } catch (IllegalArgumentException e) {
            System.out.println("\nErro ao decodificar arquivo recebido de " + sender + ": " + e.getMessage());
            return;
        }

        Path downloadsPath = Paths.get(DOWNLOADS_DIRECTORY);
        Path targetPath = resolveFilePath(downloadsPath, originalFileName);

        try {
            Files.createDirectories(downloadsPath);
            Files.write(targetPath, fileData);
            System.out.println("\nArquivo recebido de " + sender + ": " + targetPath.getFileName() + " (" + fileData.length + " bytes)");
            System.out.println("Salvo em: " + targetPath.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("\nErro ao salvar arquivo recebido de " + sender + ": " + e.getMessage());
        }
    }

    private Path resolveFilePath(Path directory, String fileName) {
        Path candidate = directory.resolve(fileName);
        if (!Files.exists(candidate)) {
            return candidate;
        }

        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        int counter = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(baseName + "(" + counter++ + ")" + extension);
        }

        return candidate;
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
