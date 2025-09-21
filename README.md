# Socket Chat

## 📖 Visão Geral
**Socket Chat** é uma aplicação de mensagens instantâneas baseada em **sockets TCP**, composta por:
- um **servidor multi-thread** desenvolvido em **Spring Boot**, e
- um **cliente de linha de comando (CLI)**.

Com ele, é possível:
- trocar **mensagens privadas**,
- participar de **grupos de conversa**,
- realizar **transferência de arquivos**, e
- garantir a **entrega de mensagens offline**.

O servidor escuta conexões na porta padrão `12345` e mantém um **pool de threads** para atender múltiplos clientes simultaneamente.
- Grupos e usuários são gerenciados em memória, com mecanismos de presença (on-line/off-line).
- Mensagens destinadas a usuários offline são armazenadas e entregues automaticamente quando o usuário se reconecta.

O cliente CLI oferece uma interface baseada em comandos, mantém uma pasta local `client_downloads/` para arquivos recebidos e carrega automaticamente mensagens pendentes no login.

---

## 🏗️ Arquitetura

- **Commons**  
  Modelos compartilhados serializáveis (mensagens, usuários, grupos).  
  Incluem suporte a anexos via Base64 e estruturas thread-safe.

- **Servidor (Spring Boot)**
   - Mantém um `ServerSocket` na porta `12345`.
   - Cada nova conexão gera um `ChatHandler`, responsável por autenticação, roteamento de mensagens e administração de grupos.
   - Possui serviços para:
      - envio de mensagens privadas,
      - criação/entrada/saída de grupos,
      - armazenamento e entrega de mensagens offline,
      - transferência de arquivos.

- **Cliente (CLI)**
   - Interface interativa baseada em comandos (`/msg`, `/group`, `/file`, etc.).
   - Conecta ao servidor via host/porta configuráveis (`CHAT_SERVER_HOST`, `CHAT_SERVER_PORT`).
   - Gerencia automaticamente diretórios locais para armazenar arquivos recebidos.

---

## ⚙️ Pré-requisitos

- **Java 21+**
- **Maven 3.9+** (ou utilize o *wrapper* `./mvnw`)
- **Docker 24+** e **Docker Compose 2+** (opcional, para execução containerizada)

O projeto já inclui o **Maven Wrapper**, permitindo rodar os comandos sem precisar instalar o Maven globalmente.

---

## 🚀 Como executar localmente

### 1. Compilar o projeto
```bash
./mvnw clean package
```

> Este comando gera dois JARs:
> - Servidor: `socket-chat-0.0.1-SNAPSHOT.jar`
> - Cliente: `socket-chat-0.0.1-SNAPSHOT-client.jar`

### 2. Iniciar o servidor
```bash
java -jar target/socket-chat-0.0.1-SNAPSHOT.jar
```
O servidor ficará disponível na porta `12345`.

### 3. Iniciar o cliente CLI
```bash
java -jar target/socket-chat-0.0.1-SNAPSHOT-client.jar
```
Por padrão, conecta em `localhost:12345`, mas pode ser configurado via:
- Variáveis de ambiente: `CHAT_SERVER_HOST`, `CHAT_SERVER_PORT`
- Propriedades JVM: `-Dchat.server.host=... -Dchat.server.port=...`

Arquivos recebidos serão salvos automaticamente em `client_downloads/`.

---

## 💻 Comandos do Cliente

- `/msg <usuario> <mensagem>` – Envia mensagem privada.
- `/group <grupo> <mensagem>` – Envia mensagem para um grupo.
- `/create <grupo>` – Cria um novo grupo.
- `/join <grupo>` – Entra em um grupo existente.
- `/leave <grupo>` – Sai de um grupo.
- `/file <destino> <caminho>` – Envia arquivos para usuários ou grupos (`#grupo`).
- `/users` – Lista usuários online.
- `/groups` – Lista grupos disponíveis.
- `/help` – Mostra ajuda.
- `/quit` – Encerra a sessão.

---

## 🐳 Execução com Docker

O projeto inclui **Dockerfiles** para servidor e cliente, além de um `docker-compose.yml`.

### 1. Subir servidor
```bash
docker compose up -d --build server
```

### 2. Executar cliente interativo
```bash
docker compose run --rm client
```

Você pode abrir várias instâncias do cliente para simular múltiplos usuários.

---

## 📂 Estrutura de Diretórios

```
src/
├── main
│   ├── java/br/com/study/socketchat
│   │   ├── client/        # Cliente CLI
│   │   ├── commons/       # Modelos compartilhados
│   │   └── server/        # Servidor e serviços
│   └── resources/
│       └── application.properties
```

---

## ✅ Dicas adicionais
- O servidor remove automaticamente grupos vazios para evitar canais inativos.
- Mensagens enviadas a usuários offline são entregues no próximo login.

---