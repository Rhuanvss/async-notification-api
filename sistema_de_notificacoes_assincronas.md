# NotifyHub — Sistema de Notificações Assíncronas

## Objetivo
API REST em Java 21 + Spring Boot que recebe requisições de envio de notificação, persiste no PostgreSQL com status `PENDENTE`, publica numa fila RabbitMQ e retorna `202 Accepted`. Um Consumer interno processa a fila, simula o envio de e-mail e atualiza o status para `ENVIADO` ou `FALHA`.

---

## Stack
| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.x |
| Mensageria | RabbitMQ + Spring AMQP |
| Persistência | PostgreSQL + Spring Data JPA |
| Build | Maven |
| Infra | Docker + Docker Compose |
| Utilitários | Lombok, MapStruct (opcional) |

---

## Estrutura de Pacotes

```
src/main/java/com/notifyhub/
├── controller/
│   └── NotificationController.java
├── service/
│   ├── NotificationService.java
│   └── NotificationConsumer.java
├── repository/
│   └── NotificationRepository.java
├── domain/
│   ├── Notification.java          # @Entity
│   ├── NotificationStatus.java    # enum: PENDENTE, ENVIADO, FALHA
│   └── dto/
│       ├── NotificationRequestDTO.java
│       └── NotificationResponseDTO.java
└── config/
    └── RabbitMQConfig.java
```

---

## Modelagem da Entidade

```java
// domain/Notification.java
@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String recipient; // e-mail do destinatário

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.status = NotificationStatus.PENDENTE;
    }
}
```

---

## Fluxo de Dados

```
POST /api/notifications
        │
        ▼
NotificationController
        │  chama
        ▼
NotificationService.send()
        ├─ 1. Salva no PostgreSQL  → status: PENDENTE
        ├─ 2. Publica no RabbitMQ  → fila: notifications.queue
        └─ 3. Retorna 202 Accepted + { id, status }

        [assíncrono]
        ▼
NotificationConsumer.consume()
        ├─ 1. Lê mensagem da fila (UUID da notificação)
        ├─ 2. Busca no banco pelo ID
        ├─ 3. Simula envio de e-mail (log / Thread.sleep)
        └─ 4. Atualiza status → ENVIADO ou FALHA
```

---

## Endpoints

| Método | Path | Descrição | Response |
|---|---|---|---|
| `POST` | `/api/notifications` | Envia notificação para a fila | `202 Accepted` |
| `GET` | `/api/notifications/{id}` | Consulta status de uma notificação | `200 OK` |
| `GET` | `/api/notifications` | Lista todas as notificações | `200 OK` |

---

## Configuração RabbitMQ

```java
// config/RabbitMQConfig.java
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE     = "notifications.queue";
    public static final String EXCHANGE  = "notifications.exchange";
    public static final String ROUTING_KEY = "notifications.send";

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

---

## docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: notifyhub-postgres
    environment:
      POSTGRES_DB: notifyhub
      POSTGRES_USER: ${DB_USERNAME:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-change_me}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    container_name: notifyhub-rabbitmq
    ports:
      - "5672:5672"    # AMQP
      - "15672:15672"  # Management UI → http://localhost:15672
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USERNAME:-guest}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD:-change_me}

volumes:
  pgdata:
```

---

## application.yml (base)

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/notifyhub}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD}

server:
  port: 8080
```

---

## Dependências Maven (pom.xml — relevantes)

```xml
<!-- Spring Boot Starters -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

---

## Ordem de Implementação

1. `docker-compose.yml` → subir Postgres + RabbitMQ
2. `Notification.java` + `NotificationStatus.java` + DTOs
3. `NotificationRepository.java`
4. `RabbitMQConfig.java`
5. `NotificationService.java` (persistir + publicar)
6. `NotificationController.java` (POST + GETs)
7. `NotificationConsumer.java` (consumir + atualizar status)
8. Testes manuais via Postman/cURL
9. `application.yml` para profile `docker` (hosts internos)
10. Dockerfile da aplicação + integrar no `docker-compose.yml`
