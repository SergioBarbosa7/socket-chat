# Socket Chat

Socket Chat é uma aplicação de chat distribuído composta por um servidor TCP e um cliente de linha de comando. O projeto foi migrado para o ecossistema Spring Boot para permitir injeção de dependências e facilitar a configuração dos componentes principais do servidor, mantendo toda a comunicação baseada em sockets.

## Arquitetura

- **Servidor** (`SocketServerChatApplication`)
  - Mantém um `ServerSocket` escutando a porta `12345`.
  - Cada nova conexão cria um `ChatHandler` (escopo *prototype*) injetado com `SessionManager`, `ChatService` e `GroupService`.
  - Permite mensagens privadas, criação/entrada/saída de grupos e entrega de arquivos.
  - Armazena mensagens offline através de `OfflineMessageStorage` quando o destinatário está desconectado.
- **Cliente** (`ChatClientApplication`)
  - CLI interativa que envia comandos e mensagens ao servidor.
  - Resolve `CHAT_SERVER_HOST` e `CHAT_SERVER_PORT` via variáveis de ambiente ou propriedades do sistema para facilitar execução em contêiner.

## Pré-requisitos

- Java 21+
- Maven 3.9+ (ou o *wrapper* `./mvnw`)
- Docker 24+ e Docker Compose 2+ (opcional, para execução containerizada)

## Como executar localmente

### 1. Compilar o projeto

```bash
./mvnw clean package
```

> **Observação:** o wrapper fará download das dependências do Maven na primeira execução. Caso esteja sem acesso à internet, use um cache local ou configure um mirror acessível.

### 2. Iniciar o servidor

```bash
java -jar target/socket-chat-0.0.1-SNAPSHOT.jar
```

### 3. Iniciar um cliente CLI

Em outro terminal, execute:

```bash
java -jar target/socket-chat-0.0.1-SNAPSHOT-client.jar
```

Informe um nome de usuário quando solicitado e utilize os comandos `/help`, `/msg`, `/groups`, `/join`, entre outros, para interagir.

## Execução com Docker

O projeto inclui Dockerfiles dedicados para servidor e cliente, além de um `docker-compose.yml` para orquestração.

### 1. Construir e subir o servidor

```bash
docker compose up -d --build server
```

### 2. Abrir clientes interativos

```bash
docker compose run --rm client
```

Repita o comando acima em quantas janelas de terminal quiser para simular múltiplos usuários. Também é possível escalar clientes em segundo plano:

```bash
docker compose up --scale client=3
```

Depois conecte-se a cada contêiner em execução com `docker attach socket-chat-client-1`, `socket-chat-client-2`, etc. Use `Ctrl + P`, `Ctrl + Q` para se desanexar sem encerrar o processo.

## Como testar

1. **Testes unitários/integração**
   ```bash
   ./mvnw test
   ```
2. **Verificação de pacote executável**
   ```bash
   ./mvnw clean package
   ```
3. **Smoke test manual**
   - Inicie o servidor localmente (`java -jar target/...jar`).
   - Abra dois terminais com o cliente e verifique o envio/recebimento de mensagens privadas e em grupo.

> Nos ambientes onde não há acesso à Maven Central, os comandos acima podem falhar no download das dependências. Nesses casos utilize um repositório local ou rode os testes em um ambiente com acesso à internet.

## Comandos úteis no cliente

- `/help` – Lista todos os comandos disponíveis.
- `/msg <usuário> <mensagem>` – Envia mensagem privada.
- `/groups` – Lista grupos disponíveis.
- `/create <nome>` – Cria um novo grupo.
- `/join <nome>` – Entra em um grupo existente.
- `/leave <nome>` – Sai de um grupo.
- `/file <usuário> <caminho>` – Envia arquivo privado.
- `/quit` – Desconecta do servidor.

## Estrutura de pastas

```
.
├── Dockerfile.client
├── Dockerfile.server
├── docker-compose.yml
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   └── resources
    └── test
```

O diretório `src/main/java/br/com/study/socketchat/server` contém todas as classes do servidor Spring Boot e `src/main/java/br/com/study/socketchat/client` contém o cliente CLI.

## Licença

Este projeto é fornecido para fins de estudo e pode ser utilizado conforme necessário.
