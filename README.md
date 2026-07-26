# IquenoBot - CRM Omnicanal con IA

> Sistema SaaS Multi-Tenant para atención al cliente mediante WhatsApp con Inteligencia Artificial

## 🏗️ Arquitectura

- **Clean Architecture** con Domain Driven Design (DDD)
- **Multi-Tenancy** con aislamiento completo de datos
- **Desacoplamiento** de proveedores (WhatsApp, IA)
- **Patrón Repository, Service Layer, DTO**
- **SOLID Principles & Clean Code**

## 🚀 Stack Tecnológico

### Backend
- Java 21
- Spring Boot 3.4.4
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL 15
- Flyway (migraciones)
- MapStruct (mapping)
- Lombok
- OpenAPI/Swagger

### Infraestructura
- Docker & Docker Compose
- Maven

## 📋 Requisitos Previos

- Java 21 JDK
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL 15+ (o usar Docker)

## ⚙️ Configuración

### 1. Clonar el repositorio

```bash
git clone <repository-url>
cd iquenobot
```

### 2. Configurar variables de entorno

Crear archivo `.env` en la raíz del proyecto:

```env
# Database
DATASOURCE_URL=jdbc:postgresql://localhost:5432/iquenobot
DATASOURCE_USERNAME=postgres
DATASOURCE_PASSWORD=postgres

# JWT
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production-min-256-bits
JWT_ACCESS_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000

# Crypto
CRYPTO_SECRET=your-crypto-secret-key-change-this

# CORS
CORS_ORIGINS=*

# Logs
LOG_LEVEL=INFO

# WhatsApp Evolution API (opcional)
WHATSAPP_EVOLUTION_BASE_URL=http://localhost:8080
WHATSAPP_EVOLUTION_API_KEY=

# OpenAI (opcional)
OPENAI_API_KEY=
OPENAI_MODEL=gpt-4o-mini
```

### 3. Ejecutar con Docker Compose (Recomendado)

```bash
# Construir y ejecutar
docker-compose up -d

# Ver logs
docker-compose logs -f app

# Detener
docker-compose down
```

### 4. Ejecutar localmente (Desarrollo)

```bash
# Iniciar PostgreSQL con Docker
docker run --name postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=iquenobot -p 5432:5432 -d postgres:15-alpine

# Compilar
mvn clean package -DskipTests

# Ejecutar
java -jar target/iquenobot-1.0.0.jar

# O con Maven
mvn spring-boot:run
```

## 📚 Documentación API

Una vez iniciada la aplicación, acceder a:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## 🔐 Autenticación

### Crear Tenant (Empresa)

```bash
POST /api/v1/auth/tenants
Content-Type: application/json

{
  "companyName": "Mi Empresa",
  "subdomain": "miempresa",
  "contactEmail": "admin@miempresa.com",
  "contactPhone": "+1234567890",
  "timezone": "America/Mexico_City",
  "currency": "MXN",
  "subscriptionPlan": "trial",
  "maxUsers": 10,
  "maxConversations": 1000,
  "adminEmail": "admin@miempresa.com",
  "adminPassword": "Admin123!@#",
  "adminFirstName": "Admin",
  "adminLastName": "Usuario"
}
```

### Login

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@miempresa.com",
  "password": "Admin123!@#"
}
```

Respuesta:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "uuid-refresh-token",
    "tokenType": "Bearer",
    "expiresAt": "2024-01-01T12:00:00",
    "user": {...},
    "tenant": {...}
  }
}
```

### Usar el Token

Incluir en todas las peticiones:
```
Authorization: Bearer <accessToken>
```

## 🗂️ Estructura del Proyecto

```
src/main/java/com/iquenobot/
├── auth/                    # Autenticación y usuarios
│   ├── application/        # Servicios
│   ├── domain/            # Entidades, DTOs, Repositorios
│   ├── infrastructure/    # Implementaciones
│   └── interfaces/        # Controllers, Mappers
├── contact/               # Gestión de contactos
├── conversation/          # Conversaciones y mensajes
├── ai/                    # Inteligencia Artificial
│   ├── domain/service/   # Interfaces (IAIProvider, IWhatsAppProvider)
│   └── infrastructure/   # Implementaciones
├── shared/               # Compartido
│   ├── common/          # Entidades base
│   ├── config/          # Configuraciones
│   ├── domain/dto/      # DTOs comunes
│   ├── enums/           # Enumeraciones
│   ├── exception/       # Excepciones
│   └── security/        # Seguridad
└── config/              # Configuraciones globales

src/main/resources/
├── db/migration/        # Migraciones Flyway
├── application.yml      # Configuración principal
└── application-docker.yml
```

## 🧩 Módulos Principales

### 1. Autenticación (auth)
- Multi-tenant completo
- JWT con refresh tokens
- Roles: SUPER_ADMIN, TENANT_ADMIN, SUPERVISOR, AGENT, BOT
- Sesiones de usuario
- Bloqueo de cuentas por intentos fallidos

### 2. Contactos (contact)
- CRUD completo
- Búsqueda avanzada
- Estados: ACTIVE, INACTIVE, BLOCKED, ARCHIVED
- Campos personalizados
- Tags

### 3. Conversaciones (conversation)
- Multi-canal (WhatsApp, Telegram, etc.)
- Mensajes con adjuntos
- Estados: OPEN, IN_PROGRESS, PENDING, RESOLVED, CLOSED
- Prioridades: LOW, MEDIUM, HIGH, URGENT
- Asignación de agentes
- Métricas (tiempo de respuesta, resolución)
- Satisfacción del cliente

### 4. WhatsApp (Desacoplado)
- Interface `IWhatsAppProvider`
- Implementación: Evolution API
- Preparado para: Cloud API, Baileys, Twilio
- Webhooks
- Mensajes multimedia

### 5. Inteligencia Artificial (Desacoplado)
- Interface `IAIProvider`
- Implementación: OpenAI
- Preparado para: Groq, Gemini, Claude
- Chat completion
- Detección de intenciones
- Análisis de sentimiento
- Embeddings

## 🔒 Seguridad

- ✅ JWT con rotación de tokens
- ✅ Bcrypt para contraseñas
- ✅ Multi-tenancy con filtros Hibernate
- ✅ CORS configurado
- ✅ Rate limiting (pendiente implementar)
- ✅ SQL Injection protegido (JPA)
- ✅ XSS protegido (Spring Security)
- ✅ Variables de entorno para secretos

## 📊 Base de Datos

### Migraciones Flyway

Las migraciones se ejecutan automáticamente al iniciar la aplicación:

- `V001__Create_tenants_table.sql`
- `V002__Create_users_table.sql`
- `V003__Create_refresh_tokens_table.sql`
- `V004__Create_user_sessions_table.sql`
- `V005__Create_contacts_table.sql`
- `V006__Create_conversations_table.sql`
- `V007__Create_conversation_messages_table.sql`
- `V008__Create_message_attachments_table.sql`

### Modelo de Datos

- **Multi-Tenancy**: Cada tabla tiene `tenant_id`
- **Auditoría**: `created_at`, `updated_at`, `created_by`, `updated_by`
- **Soft Delete**: `deleted_at`, `deleted_by`, `is_deleted`
- **Optimistic Locking**: `version`
- **UUIDs**: IDs únicos globales

## 🧪 Testing

```bash
# Ejecutar tests
mvn test

# Ejecutar tests con coverage
mvn test jacoco:report
```

## 🚢 Despliegue

### Railway

```bash
# Instalar Railway CLI
npm install -g @railway/cli

# Login
railway login

# Inicializar proyecto
railway init

# Agregar PostgreSQL
railway add

# Configurar variables de entorno
railway variables set JWT_SECRET=your-secret

# Deploy
railway up
```

### Render

1. Conectar repositorio GitHub
2. Configurar como **Web Service**
3. Build Command: `mvn clean package -DskipTests`
4. Start Command: `java -jar target/iquenobot-1.0.0.jar`
5. Agregar PostgreSQL database
6. Configurar variables de entorno

### AWS (EC2 + RDS)

Ver documentación en `docs/aws-deployment.md`

## 📈 Métricas y Monitoreo

Actuator endpoints:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Info: `/actuator/info`

Métricas del Orchestrator (Micrometer):
- `orchestrator.messages.processed` - Mensajes procesados
- `orchestrator.messages.failed` - Mensajes fallidos
- `orchestrator.processing.time` - Tiempo de procesamiento
- `orchestrator.decisions` - Decisiones tomadas
- `orchestrator.actions.executed` - Acciones ejecutadas
- `orchestrator.webhooks.received` - Webhooks recibidos

## 🧠 Conversation Orchestrator

El **Orchestrator** es el cerebro del CRM. Todos los mensajes de cualquier canal pasan obligatoriamente por él.

### Características

✅ **Punto de Entrada Único**: WhatsApp, Telegram, Messenger, Instagram, Email, SMS, Webchat  
✅ **Pipeline de Procesamiento**: Validación → Resolución → Persistencia → Decisión → Ejecución  
✅ **Decision Engine**: Motor de decisiones con múltiples estrategias  
✅ **Action Executors**: Ejecución desacoplada de acciones (Strategy Pattern)  
✅ **Event System**: Eventos para Analytics, Notifications, Audit  
✅ **Métricas en Tiempo Real**: Micrometer + Actuator  
✅ **Auditoría Completa**: Trazabilidad de todas las operaciones  

### Flujo de Procesamiento

```
Webhook Recibido
      ↓
Validation Step
      ↓
Tenant Resolution Step
      ↓
Contact Resolution Step  
      ↓
Conversation Resolution Step
      ↓
Message Persistence Step
      ↓
Metrics Update Step
      ↓
Decision Engine
      ↓
Action Dispatcher
      ↓
Action Executor
      ↓
Audit Step
      ↓
Event Publishing
```

### Estrategias de Decisión

1. **BotDecisionStrategy**: Respuestas automáticas con IA/Templates
2. **AgentAssignmentStrategy**: Asignación inteligente de agentes
3. **AutoResponseStrategy**: Respuestas rápidas predefinidas

### Acciones Disponibles

- `SEND_TEXT` - Enviar mensaje de texto
- `SEND_MEDIA` - Enviar archivos/multimedia
- `CREATE_LEAD` - Crear lead desde conversación
- `ASSIGN_AGENT` - Asignar agente humano
- `TRANSFER_CONVERSATION` - Transferir a otro agente
- `TAG_CONVERSATION` - Etiquetar conversación
- `CLOSE_CONVERSATION` - Cerrar conversación
- `SCHEDULE_MESSAGE` - Programar mensaje
- `NO_ACTION` - No hacer nada

### Eventos del Sistema

- `MessageReceivedEvent` - Mensaje recibido
- `ConversationCreatedEvent` - Conversación creada
- `AgentAssignedEvent` - Agente asignado
- `BotAnsweredEvent` - Bot respondió
- `ConversationClosedEvent` - Conversación cerrada
- `ActionExecutedEvent` - Acción ejecutada

### Extensibilidad

Para agregar una nueva acción:

1. Crear clase que implemente `ActionExecutor`
2. Anotar con `@Component`
3. Listo! Se registra automáticamente

```java
@Component
@RequiredArgsConstructor
public class CustomActionExecutor implements ActionExecutor {
    @Override
    public ActionType getActionType() {
        return ActionType.CUSTOM_ACTION;
    }
    
    @Override
    public void execute(Decision decision, ProcessingContext context) {
        // Tu lógica aquí
    }
}
```

### Audit Logs API

Endpoints para consultar auditoría:

```bash
# Obtener todos los logs
GET /api/v1/orchestrator/audit

# Logs de una conversación
GET /api/v1/orchestrator/audit/conversation/{id}

# Logs por canal
GET /api/v1/orchestrator/audit/channel/WHATSAPP

# Operaciones fallidas
GET /api/v1/orchestrator/audit/failed

# Estadísticas
GET /api/v1/orchestrator/audit/stats
```

## 🤝 Contribuir

1. Fork el proyecto
2. Crear feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push al branch (`git push origin feature/AmazingFeature`)
5. Abrir Pull Request

## 📝 Licencia

Proprietary - Todos los derechos reservados

## 👥 Equipo

- **Arquitectura**: Clean Architecture + DDD
- **Backend**: Spring Boot + Java 21
- **Database**: PostgreSQL con Flyway
- **Security**: JWT + Multi-Tenancy
- **AI**: Desacoplado (OpenAI, Groq, Gemini, Claude)
- **WhatsApp**: Desacoplado (Evolution API, Cloud API, Baileys, Twilio)

## 📞 Soporte

Para soporte, enviar email a support@iquenobot.com

---

**Construido con ❤️ siguiendo las mejores prácticas empresariales**