# Arquitectura del Sistema

## 🏛️ Principios Arquitectónicos

### 1. Clean Architecture
El proyecto sigue los principios de Clean Architecture de Robert C. Martin:

```
┌─────────────────────────────────────────┐
│         Interfaces (Controllers)        │  ← API REST, Webhooks
├─────────────────────────────────────────┤
│       Application (Use Cases)           │  ← Servicios de negocio
├─────────────────────────────────────────┤
│      Domain (Entities, DTOs, Repos)     │  ← Lógica de dominio
├─────────────────────────────────────────┤
│   Infrastructure (Implementations)       │  ← Integraciones externas
└─────────────────────────────────────────┘
```

### 2. Domain Driven Design (DDD)

Cada módulo representa un **Bounded Context** con:
- **Entities**: Objetos de negocio con identidad
- **Value Objects**: DTOs inmutables
- **Repositories**: Interfaces para persistencia
- **Services**: Lógica de aplicación
- **Mappers**: Transformaciones entre capas

### 3. Organización por Módulos

```
module/
├── application/        # Casos de uso y servicios
├── domain/            
│   ├── entity/        # Entidades JPA
│   ├── dto/           # DTOs de transferencia
│   └── repository/    # Interfaces de repositorio
├── infrastructure/     # Implementaciones externas
└── interfaces/        
    ├── controller/    # REST Controllers
    └── mapper/        # MapStruct mappers
```

## 🏢 Multi-Tenancy

### Estrategia: Shared Database, Shared Schema

**Decisión**: Se eligió "Shared Database, Shared Schema" por:

✅ **Ventajas**:
- Menor costo de infraestructura
- Mantenimiento simplificado (una sola base de datos)
- Migraciones unificadas
- Backup y restore más sencillo
- Escalabilidad vertical

⚠️ **Consideraciones**:
- Aislamiento mediante `tenant_id` en todas las tablas
- Filtros Hibernate automáticos
- Validaciones estrictas en capa de servicio

### Implementación

1. **TenantContext** (ThreadLocal)
```java
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();
}
```

2. **JWT con tenant_id**
```json
{
  "sub": "user-uuid",
  "tenantId": "tenant-uuid",
  "roles": ["TENANT_ADMIN"],
  "exp": 1234567890
}
```

3. **Hibernate Filters**
```java
@FilterDef(name = "tenantFilter", 
    parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Entity extends BaseEntity {
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
}
```

## 🔐 Seguridad

### Autenticación

1. **Login** → Genera Access Token + Refresh Token
2. **Access Token** → JWT (15 min de vida)
3. **Refresh Token** → UUID en DB (7 días)

### Autorización

**Roles jerárquicos**:
```
SUPER_ADMIN
    └── TENANT_ADMIN
            └── SUPERVISOR
                    └── AGENT
                            └── BOT
```

**Permisos**:
- `@PreAuthorize("hasRole('TENANT_ADMIN')")`
- `@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")`

### Filtro de Seguridad

```
Request → JwtAuthenticationFilter 
       → TenantContext.set(tenantId, userId)
       → TenantFilter (Hibernate)
       → Controller
       → Service (validates tenant)
       → Repository (filtered by tenant_id)
```

## 🔌 Desacoplamiento de Proveedores

### WhatsApp Provider (Strategy Pattern)

```java
public interface IWhatsAppProvider {
    String sendMessage(String instanceId, WhatsAppMessageDto message);
    WhatsAppProvider getProviderType();
}
```

**Implementaciones**:
- ✅ `EvolutionApiProvider` (implementado)
- 🔜 `WhatsAppCloudApiProvider` (preparado)
- 🔜 `BaileysProvider` (preparado)
- 🔜 `TwilioProvider` (preparado)

### AI Provider (Strategy Pattern)

```java
public interface IAIProvider {
    AIResponseDto chatCompletion(List<AIMessageDto> messages, ...);
    AIProvider getProviderType();
}
```

**Implementaciones**:
- ✅ `OpenAIProvider` (implementado)
- 🔜 `GroqProvider` (preparado)
- 🔜 `GeminiProvider` (preparado)
- 🔜 `ClaudeProvider` (preparado)

**Ventajas**:
- Sin vendor lock-in
- Cambio de proveedor sin modificar lógica de negocio
- Múltiples proveedores simultáneos (fallback)
- Testing con mocks sencillo

## 📊 Modelo de Datos

### Entidades Base

```java
@MappedSuperclass
public abstract class BaseEntity {
    private UUID id;
    private UUID tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Long version; // Optimistic locking
}

@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {
    private LocalDateTime deletedAt;
    private UUID deletedBy;
    private boolean deleted = false;
}
```

### Relaciones Clave

```
Tenant
  └── Users
  └── Contacts
        └── Conversations
              └── Messages
                    └── Attachments
```

## 🎯 Patrones de Diseño

### 1. Repository Pattern
```java
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Page<Conversation> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
}
```

### 2. Service Layer Pattern
```java
@Service
@Transactional
public class ConversationService {
    private final ConversationRepository repository;
    private final ConversationMapper mapper;
    
    public ConversationDto create(CreateConversationRequestDto request) {
        // Business logic
    }
}
```

### 3. DTO Pattern
```java
// Nunca exponer entidades directamente
@Data
public class ConversationDto {
    private UUID id;
    private ContactDto contact;
    private UserDto assignedUser;
    // ...
}
```

### 4. Builder Pattern
```java
Conversation conversation = Conversation.builder()
    .id(UUID.randomUUID())
    .tenantId(tenantId)
    .status(ConversationStatus.OPEN)
    .build();
```

### 5. Strategy Pattern
```java
// Selección dinámica de proveedor
IAIProvider provider = aiProviderFactory.getProvider(AIProvider.OPENAI);
AIResponseDto response = provider.chatCompletion(messages, ...);
```

## 🚀 Escalabilidad

### Horizontal Scaling

✅ **Stateless Application**
- Sin sesiones en servidor
- JWT para autenticación
- Todo en base de datos o cache

✅ **Database Connection Pooling**
```yaml
hikari:
  maximum-pool-size: 20
  minimum-idle: 5
```

✅ **Virtual Threads** (Java 21)
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### Vertical Scaling

- Optimistic Locking para concurrencia
- Índices en columnas frecuentes
- Query optimization (N+1 evitado)
- Lazy loading estratégico

### Futuro

🔜 **Cache** (Redis)
- Session cache
- Query result cache
- Rate limiting

🔜 **Message Queue** (RabbitMQ/Kafka)
- Async message processing
- Webhook processing
- Email sending

🔜 **CDN** (CloudFlare)
- Static assets
- Media files

## 📈 Monitoreo y Observabilidad

### Actuator Endpoints

```
/actuator/health     → Health check
/actuator/metrics    → Métricas JVM
/actuator/info       → Información app
```

### Logging

```java
@Slf4j
public class Service {
    public void method() {
        log.info("Operation started for tenant: {}", tenantId);
        log.error("Error occurred: {}", e.getMessage(), e);
    }
}
```

### Métricas Clave

- Tiempo de respuesta promedio
- Tasa de error
- Conexiones de DB activas
- Uso de memoria/CPU
- Requests por segundo

## 🧪 Testing Strategy

### Unit Tests
```java
@Test
void shouldCreateConversation() {
    // Given
    CreateConversationRequestDto request = ...;
    
    // When
    ConversationDto result = service.create(request);
    
    // Then
    assertThat(result.getId()).isNotNull();
}
```

### Integration Tests
```java
@SpringBootTest
@Testcontainers
class ConversationIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = ...;
}
```

## 📝 Convenciones de Código

### Naming

- **Entities**: `User`, `Conversation` (singular)
- **Tables**: `users`, `conversations` (plural)
- **DTOs**: `UserDto`, `CreateUserRequestDto`
- **Services**: `UserService`, `ConversationService`
- **Repositories**: `UserRepository`
- **Controllers**: `UserController`

### Package Structure

```
com.iquenobot.{module}.{layer}.{type}
```

Ejemplos:
- `com.iquenobot.auth.application.AuthService`
- `com.iquenobot.conversation.domain.entity.Conversation`
- `com.iquenobot.contact.interfaces.controller.ContactController`

---

**Esta arquitectura está diseñada para soportar**:
- ✅ Miles de tenants
- ✅ Millones de mensajes
- ✅ Alta concurrencia
- ✅ Fácil mantenimiento
- ✅ Extensibilidad sin modificar código existente