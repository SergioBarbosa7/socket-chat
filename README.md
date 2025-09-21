# Socket Chat

## Visão geral
Socket Chat é uma aplicação de mensagens instantâneas baseada em sockets composta por um servidor Spring Boot multi-thread e um cliente de linha de comando que permite conversas privadas, comunicação em grupos e transferência de arquivos.【F:src/main/java/br/com/study/socketchat/server/SocketServerChatApplication.java†L20-L95】【F:src/main/java/br/com/study/socketchat/server/ChatHandler.java†L114-L237】【F:src/main/java/br/com/study/socketchat/client/ChatClientApplication.java†L17-L335】

O servidor aceita conexões na porta `12345`, mantém um pool de threads para atender vários clientes simultaneamente e cria automaticamente o diretório `server_files/` para armazenar anexos recebidos.【F:src/main/java/br/com/study/socketchat/server/SocketServerChatApplication.java†L27-L85】 As sessões de usuários são registradas com indicação de presença on-line/off-line, e mensagens destinadas a usuários desconectados são persistidas temporariamente para entrega posterior.【F:src/main/java/br/com/study/socketchat/server/session/SessionManager.java†L19-L71】【F:src/main/java/br/com/study/socketchat/server/service/ChatService.java†L74-L111】【F:src/main/java/br/com/study/socketchat/server/storage/impl/OfflineMessageStorageImpl.java†L10-L34】

O cliente CLI negocia automaticamente o host/porta via variáveis de ambiente, mantém um diretório local `client_downloads/` para arquivos recebidos e oferece comandos interativos para envio de mensagens, gerenciamento de grupos e compartilhamento de arquivos.【F:src/main/java/br/com/study/socketchat/client/ChatClientApplication.java†L23-L352】【F:src/main/java/br/com/study/socketchat/client/ChatClientApplication.java†L404-L452】 Após um login bem-sucedido, mensagens offline são entregues automaticamente ao usuário.【F:src/main/java/br/com/study/socketchat/server/ChatHandler.java†L91-L112】

## Componentes principais
- **Commons** – Modelos compartilhados serializáveis para mensagens, usuários e grupos; incluem suporte a anexos via Base64 e coleções thread-safe para membros de grupo.【F:src/main/java/br/com/study/socketchat/commons/Message.java†L11-L44】【F:src/main/java/br/com/study/socketchat/commons/User.java†L9-L32】【F:src/main/java/br/com/study/socketchat/commons/Group.java†L12-L59】
- **Servidor** – Gerencia autenticação, roteamento de mensagens privadas, administração de grupos e envio de arquivos, reutilizando handlers por conexão de socket.【F:src/main/java/br/com/study/socketchat/server/ChatHandler.java†L20-L237】【F:src/main/java/br/com/study/socketchat/server/group/GroupManager.java†L11-L57】
- **Cliente** – Interface interativa baseada em comandos com feedback textual e download automático de arquivos recebidos.【F:src/main/java/br/com/study/socketchat/client/ChatClientApplication.java†L178-L452】

## Pré-requisitos
- Java 21 e Maven 3 para compilar e executar localmente.【F:pom.xml†L5-L49】
- Docker 24+ (opcional) para utilização dos contêineres fornecidos.【F:Dockerfile.server†L1-L11】【F:docker-compose.yml†L3-L29】

O projeto inclui o Maven Wrapper (`mvnw`), permitindo rodar os comandos abaixo sem uma instalação global do Maven.

## Como executar localmente

### 1. Empacotar os artefatos
```bash
./mvnw clean package
```
Esse comando gera dois arquivos JAR: o servidor padrão (`socket-chat-0.0.1-SNAPSHOT.jar`) e o cliente com classifier `client` (`socket-chat-0.0.1-SNAPSHOT-client.jar`).【F:pom.xml†L51-L74】

### 2. Executar o servidor
```bash
java -jar target/socket-chat-0.0.1-SNAPSHOT.jar
```
O servidor ficará escutando na porta `12345` e aceitará novas conexões de clientes.【F:src/main/java/br/com/study/socketchat/server/SocketServerChatApplication.java†L27-L95】

### 3. Executar o cliente
Em um novo terminal:
```bash
java -jar target/socket-chat-0.0.1-SNAPSHOT-client.jar
```
Por padrão o cliente conecta em `localhost:12345`, mas é possível sobrescrever as configurações via `CHAT_SERVER_HOST` e `CHAT_SERVER_PORT` (variáveis de ambiente ou propriedades JVM `chat.server.host` / `chat.server.port`).【F:src/main/java/br/com/study/socketchat/client/ChatClientApplication.java†L41-L73】 Ao receber arquivos, eles serão salvos no diretório `client_downloads/` criado automaticamente.【F:src/main/java/br/com/study/socketchat/client/ChatClientApplication.java†L23-L39】【F:src/main/java/br/com/study/socketchat/client/ChatClientApplication.java†L404-L429】

## Comandos disponíveis no cliente
Todos os comandos começam com `/`:
- `/msg <usuário> <mensagem>` – Envia mensagem privada.
- `/group <grupo> <mensagem>` – Envia mensagem para um grupo existente.
- `/create <grupo>` – Cria um novo grupo com o usuário corrente como administrador.
- `/join <grupo>` – Solicita entrada em um grupo existente.
- `/leave <grupo>` – Sai de um grupo do qual você participa.
- `/file <destino> <caminho>` – Envia arquivos para usuários ou grupos (prefixe com `#` para grupos).
- `/users` – Lista usuários registrados e status on-line.
- `/groups` – Lista os grupos disponíveis.
- `/help` – Exibe a ajuda com todos os comandos.
- `/quit` – Encerra a sessão e desconecta.

Os comandos acima são processados pelo cliente, que encaminha mensagens tipadas ao servidor conforme necessário.【F:src/main/java/br/com/study/socketchat/client/ChatClientApplication.java†L178-L335】 No servidor, cada requisição dispara regras de validação, atualização de grupos e entrega de mensagens ou arquivos aos destinatários corretos.【F:src/main/java/br/com/study/socketchat/server/ChatHandler.java†L114-L237】【F:src/main/java/br/com/study/socketchat/server/service/ChatService.java†L35-L109】

## Executando com Docker
É possível subir servidor e cliente usando Docker Compose:
```bash
docker compose up --build
```
O serviço `server` expõe a porta `12345`, enquanto o serviço `client` já é configurado para apontar para o host `server` dentro da rede `chat-net` e mantém `stdin`/`tty` abertos para interação com o terminal.【F:docker-compose.yml†L3-L29】 Os Dockerfiles compilam os artefatos via Maven e executam os JARs gerados automaticamente.【F:Dockerfile.server†L1-L11】【F:Dockerfile.client†L1-L12】

## Estrutura de diretórios
```
src/
├── main
│   ├── java/br/com/study/socketchat
│   │   ├── client/        # Cliente CLI
│   │   ├── commons/       # Modelos compartilhados
│   │   └── server/        # Servidor e serviços
│   └── resources/
│       └── application.properties
└── test/java/...          # Testes automatizados
```
A estrutura acima separa claramente os módulos cliente/servidor e compartilha modelos serializáveis em `commons`.

## Testes automatizados
Execute os testes com:
```bash
./mvnw test
```
Os testes utilizam Spring Boot Test para validar o carregamento do contexto da aplicação.【F:src/test/java/br/com/study/socketchat/SocketChatApplicationTests.java†L1-L12】

## Dicas adicionais
- O servidor remove automaticamente grupos vazios para evitar acumular canais inativos.【F:src/main/java/br/com/study/socketchat/server/group/GroupManager.java†L35-L41】
- Mensagens enviadas para usuários offline são guardadas e entregues assim que o destinatário efetuar login novamente.【F:src/main/java/br/com/study/socketchat/server/service/ChatService.java†L82-L110】

Bom chat! 🎧
