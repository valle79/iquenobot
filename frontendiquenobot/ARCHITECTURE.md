# Frontend Architecture - IquenoBot CRM

## 1. Stack Decisiones Técnicas

| Tecnología | Versión | Por qué |
|------------|---------|---------|
| **React 19** | ^19.0.0 | Server Components, use(), Actions, estabilidad |
| **Vite** | ^6.x | Build speed, HMR nativo, tree-shaking |
| **TypeScript** | ^5.x | Strict mode, no `any`, tipos inferidos |
| **TailwindCSS v4** | ^4.x | CSS-first config, @theme, performance |
| **React Router DOM** | ^7.x | Loaders, actions, tipos nativos |
| **Zustand** | ^5.x | Stores atómicos, sin boilerplate, middleware |
| **TanStack Query** | ^5.x | Caching, refetch, optimistic updates, SSR |
| **Axios** | ^1.x | Interceptors, cancel tokens, progress |
| **React Hook Form + Zod** | ^7.x + ^3.x | Validación schema-based, performance |
| **Framer Motion** | ^11.x | Layout animations, gestures, AnimatePresence |
| **Lucide React** | ^0.x | Iconos tree-shakeables, consistentes |
| **Sonner** | ^1.x | Toasts minimalistas, customizable |
| **Day.js** | ^1.x | 2KB, plugins, locales |
| **TanStack Table** | ^8.x | Headless, virtualización, filtering, sorting |
| **Recharts** | ^2.x | Composable, responsive, SVG |
| **Socket.IO Client** | ^4.x | Tiempo real, auto-reconnect, namespaces |
| **i18next** | ^24.x | Traducciones, lazy loading, detección idioma |

## 2. Estructura de Carpetas

```
frontendiquenobot/
├── src/
│   ├── app/                        # App root, routing, providers
│   │   ├── App.tsx
│   │   └── main.tsx
│   │
│   ├── config/                     # Configuración global
│   │   ├── api.ts                  # Axios instance + interceptors
│   │   ├── constants.ts
│   │   ├── dayjs.ts               # Plugins dayjs
│   │   └── i18n.ts                # i18next setup
│   │
│   ├── core/                       # Lógica compartida (no UI)
│   │   ├── api/
│   │   │   ├── client.ts          # Axios instance factory
│   │   │   ├── interceptors.ts    # Auth, tenant, refresh interceptors
│   │   │   └── socket.ts          # Socket.IO client factory
│   │   ├── auth/
│   │   │   ├── auth.store.ts      # Zustand store
│   │   │   ├── auth.guard.tsx     # Route protection component
│   │   │   └── auth.service.ts    # Login, logout, refresh API calls
│   │   ├── rbac/
│   │   │   ├── permissions.ts     # Permission definitions
│   │   │   ├── rbac.store.ts      # Current user roles store
│   │   │   └── ability.ts         # Can/Can't helpers
│   │   └── websocket/
│   │       ├── socket.store.ts    # Connection state store
│   │       ├── socket.hooks.ts    # useSocket, useSocketEvent
│   │       └── events.ts          # Event type definitions
│   │
│   ├── shared/                     # Atomic Design components
│   │   ├── atoms/                 # Button, Input, Badge, Avatar, etc.
│   │   ├── molecules/             # SearchBar, Card, Dropdown, Modal
│   │   ├── organisms/             # DataTable, Sidebar, Header, Form
│   │   └── templates/             # Page layouts, error boundaries
│   │
│   ├── modules/                    # Feature-based modules
│   │   ├── authentication/
│   │   │   ├── pages/
│   │   │   │   ├── LoginPage.tsx
│   │   │   │   └── RegisterPage.tsx
│   │   │   ├── components/
│   │   │   ├── hooks/
│   │   │   └── index.ts
│   │   ├── dashboard/
│   │   │   ├── pages/
│   │   │   ├── components/
│   │   │   ├── hooks/
│   │   │   └── services/
│   │   ├── chat/                   # Módulo principal
│   │   │   ├── pages/
│   │   │   ├── components/
│   │   │   │   ├── conversation-list/
│   │   │   │   ├── chat-window/
│   │   │   │   ├── message-input/
│   │   │   │   ├── message-bubble/
│   │   │   │   ├── contact-info/
│   │   │   │   └── chat-toolbar/
│   │   │   ├── hooks/
│   │   │   ├── services/
│   │   │   └── stores/
│   │   ├── contacts/
│   │   ├── companies/
│   │   ├── products/
│   │   ├── users/
│   │   ├── roles/
│   │   ├── permissions/
│   │   ├── analytics/
│   │   ├── settings/
│   │   ├── notifications/
│   │   ├── ai/
│   │   └── tenant/
│   │
│   ├── layout/                     # Layout system
│   │   ├── AppLayout.tsx          # Main authenticated layout
│   │   ├── AuthLayout.tsx         # Login/register layout
│   │   ├── Sidebar.tsx
│   │   ├── Topbar.tsx
│   │   └── SidebarItem.tsx
│   │
│   ├── hooks/                      # Global hooks
│   │   ├── useMediaQuery.ts
│   │   ├── useDebounce.ts
│   │   ├── usePagination.ts
│   │   └── useBreakpoint.ts
│   │
│   ├── providers/                  # React context providers
│   │   ├── ThemeProvider.tsx
│   │   ├── AuthProvider.tsx
│   │   ├── SocketProvider.tsx
│   │   └── QueryProvider.tsx
│   │
│   ├── services/                   # Global API services
│   │   └── base.service.ts        # Generic CRUD service factory
│   │
│   ├── routes/
│   │   ├── index.tsx              # Route definitions
│   │   ├── protected-route.tsx    # RBAC wrapper
│   │   └── lazy-routes.ts         # Code splitting imports
│   │
│   ├── types/                      # Global TypeScript types
│   │   ├── api.ts                 # ApiResponse<T>, PagedResponse<T>
│   │   ├── enums.ts               # All shared enums
│   │   ├── auth.ts                # AuthResponse, LoginRequest, etc.
│   │   ├── chat.ts                # Conversation, Message, etc.
│   │   ├── contact.ts
│   │   ├── product.ts
│   │   ├── user.ts
│   │   ├── notification.ts
│   │   └── dashboard.ts
│   │
│   └── assets/
│       ├── images/
│       └── icons/
│
├── public/
├── index.html
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tailwind.config.ts              # Tailwind v4 CSS config
├── eslint.config.js
├── prettier.config.js
├── .env.example
└── package.json
```

## 3. Flujo de Navegación

```
/login                              → AuthLayout > LoginPage
/register                           → AuthLayout > RegisterPage
/                                   → RequireAuth > AppLayout > Dashboard
/conversations                      → RequireAuth > AppLayout > ChatPage
/conversations/:id                  → RequireAuth > AppLayout > ChatPage (conversación activa)
/contacts                           → RequireAuth > AppLayout > ContactsPage
/contacts/:id                       → RequireAuth > AppLayout > ContactDetailPage
/contacts/new                       → RequireAuth > AppLayout > ContactFormPage
/leads                              → RequireAuth > AppLayout > LeadsPage
/leads/:id                          → RequireAuth > AppLayout > LeadDetailPage
/products                           → RequireAuth > AppLayout > ProductsPage
/products/:id                       → RequireAuth > AppLayout > ProductDetailPage
/users                              → RequireAuth > AppLayout > UsersPage (TENANT_ADMIN, SUPERVISOR)
/roles                              → RequireAuth > AppLayout > RolesPage (TENANT_ADMIN)
/settings                           → RequireAuth > AppLayout > SettingsPage
/settings/company                   → RequireAuth > AppLayout > CompanySettingsPage
/settings/ai                        → RequireAuth > AppLayout > AISettingsPage
/analytics                          → RequireAuth > AppLayout > AnalyticsPage
/notifications                      → RequireAuth > AppLayout > NotificationsPage
```

## 4. Design System

### 4.1 Tokens (TailwindCSS v4 @theme)

```css
@theme {
  /* Brand */
  --color-brand-50: oklch(0.95 0.02 260);
  --color-brand-100: oklch(0.90 0.04 260);
  --color-brand-200: oklch(0.80 0.08 260);
  --color-brand-300: oklch(0.70 0.12 260);
  --color-brand-400: oklch(0.60 0.16 260);
  --color-brand-500: oklch(0.50 0.20 260);
  --color-brand-600: oklch(0.40 0.18 260);
  --color-brand-700: oklch(0.30 0.15 260);
  --color-brand-800: oklch(0.20 0.10 260);
  --color-brand-900: oklch(0.15 0.06 260);

  /* Surface (light/dark handled by Tailwind v4) */
  --color-surface: var(--color-white);
  --color-surface-secondary: var(--color-gray-50);
  --color-surface-tertiary: var(--color-gray-100);

  /* Typography */
  --font-sans: 'Inter', system-ui, sans-serif;
  --font-mono: 'JetBrains Mono', monospace;

  /* Spacing scale (already in Tailwind) */

  /* Shadows */
  --shadow-card: 0 1px 3px 0 rgb(0 0 0 / 0.06), 0 1px 2px -1px rgb(0 0 0 / 0.06);
  --shadow-elevated: 0 4px 6px -1px rgb(0 0 0 / 0.08), 0 2px 4px -2px rgb(0 0 0 / 0.06);
  --shadow-modal: 0 20px 25px -5px rgb(0 0 0 / 0.10), 0 8px 10px -6px rgb(0 0 0 / 0.10);

  /* Radius */
  --radius-xs: 4px;
  --radius-sm: 6px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-xl: 16px;

  /* Transitions */
  --ease-spring: cubic-bezier(0.16, 1, 0.3, 1);
}
```

### 4.2 Componentes Atómicos

| Componente | Props Principales | Estados |
|------------|------------------|---------|
| **Button** | variant (primary/secondary/ghost/danger), size (sm/md/lg), loading, icon, disabled | default, hover, active, loading, disabled |
| **Input** | label, error, helperText, leftIcon, rightIcon, size | default, focus, error, disabled |
| **Badge** | variant (success/warning/error/info/neutral), size, dot | - |
| **Avatar** | src, name, size, status (online/away/busy/offline) | image, fallback (initials) |
| **Select** | options, placeholder, searchable, clearable, multi | default, focus, error, disabled |
| **Toggle** | checked, onChange, size, label | on/off, disabled |
| **Spinner** | size, variant | - |
| **Skeleton** | variant (text/circular/rectangular), width, height | - |
| **Tooltip** | content, position, delay | - |
| **Dropdown** | items, align, onSelect | open/closed |

### 4.3 Componentes Moleculares

| Componente | Descripción |
|------------|-------------|
| **SearchBar** | Input + icon + debounce + clear |
| **DataCard** | Card con header, body, actions |
| **EmptyState** | Icono + título + descripción + CTA |
| **ErrorState** | Icono + mensaje + retry button |
| **Modal** | Portal + overlay + animation + sizes |
| **ConfirmDialog** | Modal de confirmación con danger variant |
| **Pagination** | Page numbers + prev/next + total |
| **FilterBar** | Conjunto de filtros con chips |
| **Tabs** | Underline/pills variants + animated indicator |
| **Breadcrumb** | Navegación jerárquica |
| **StatusDot** | Indicador de estado con colores |

### 4.4 Componentes Organísmicos

| Componente | Descripción |
|------------|-------------|
| **DataTable** | TanStack Table wrapper + sort/filter/pagination/select |
| **Sidebar** | Navegación principal con iconos + secciones |
| **Topbar** | Breadcrumb + search + actions + user menu |
| **PageHeader** | Título + descripción + breadcrumb + actions |
| **FormField** | Label + input + error + helper (RHF wrapper) |
| **FormSection** | Sección de formulario con grid |
| **StatsGrid** | Grid de tarjetas de estadísticas |
| **NotificationBell** | Bell icon + unread count + dropdown list |
| **UserMenu** | Avatar + nombre + menú desplegable |

## 5. Sistema de Estados

### 5.1 Estados Globales (Zustand)

```typescript
// Auth Store
interface AuthState {
  user: UserDto | null;
  tenant: TenantDto | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequestDto) => Promise<void>;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
  setUser: (user: UserDto) => void;
}

// UI Store
interface UIState {
  theme: 'light' | 'dark';
  sidebarOpen: boolean;
  sidebarCollapsed: boolean;
  toggleTheme: () => void;
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
}

// Socket Store
interface SocketState {
  socket: Socket | null;
  isConnected: boolean;
  connect: () => void;
  disconnect: () => void;
}

// Notification Store
interface NotificationState {
  notifications: NotificationDto[];
  unreadCount: number;
  addNotification: (n: NotificationDto) => void;
  markAsRead: (id: string) => void;
  setUnreadCount: (count: number) => void;
}
```

### 5.2 Estados por Módulo (TanStack Query)

Cada módulo usa TanStack Query para:
- **Fetching**: `useQuery` con staleTime personalizado
- **Mutations**: `useMutation` con invalidación automática
- **Optimistic Updates**: Para chat y cambios de estado
- **Pagination**: `keepPreviousData` para paginación fluida
- **Infinite Scroll**: `useInfiniteQuery` para mensajes

### 5.3 Estados de UI por Componente

```
IDLE → LOADING → SUCCESS
IDLE → LOADING → ERROR → IDLE (retry)
```

Cada componente que carga datos usa:
```typescript
type AsyncState<T> = 
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: Error };
```

## 6. Comunicación con Spring Boot

### 6.1 Axios Instance

```typescript
// config/api.ts
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
});

// Request interceptor: attach JWT + tenant
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor: refresh on 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;
      await useAuthStore.getState().refreshAuth();
      return api(error.config);
    }
    return Promise.reject(error);
  }
);
```

### 6.2 Patrón de Servicios

```typescript
// services/base.service.ts
export class BaseService<T, CreateDTO, UpdateDTO> {
  constructor(protected endpoint: string) {}

  getAll(params?: PaginationParams): Promise<PagedResponse<T>> {
    return api.get(this.endpoint, { params }).then(r => r.data.data);
  }

  getById(id: string): Promise<T> {
    return api.get(`${this.endpoint}/${id}`).then(r => r.data.data);
  }

  create(dto: CreateDTO): Promise<T> {
    return api.post(this.endpoint, dto).then(r => r.data.data);
  }

  update(id: string, dto: UpdateDTO): Promise<T> {
    return api.put(`${this.endpoint}/${id}`, dto).then(r => r.data.data);
  }

  delete(id: string): Promise<void> {
    return api.delete(`${this.endpoint}/${id}`);
  }
}
```

### 6.3 Mapeo de Respuestas

```
Backend Response:                    Frontend Extraction:
{                                   
  success: true,                    → Used by interceptor
  message: "Operación exitosa",     → Sonner toast
  data: { ... }                     → Returned as T
  timestamp: "2026-07-25T12:00:00"  → Logging
}
```

## 7. Manejo de WebSocket (Socket.IO)

### 7.1 Conexión

```typescript
// core/api/socket.ts
export function createSocket(token: string): Socket {
  return io(import.meta.env.VITE_WS_URL || 'http://localhost:8080', {
    auth: { token },
    transports: ['websocket'],
    reconnection: true,
    reconnectionAttempts: Infinity,
    reconnectionDelay: 1000,
    reconnectionDelayMax: 30000,
  });
}
```

### 7.2 Eventos

```typescript
// Eventos del servidor → cliente
interface ServerEvents {
  'conversation:new': (data: ConversationDto) => void;
  'message:new': (data: ConversationMessageDto) => void;
  'message:status': (data: { messageId: string; status: MessageStatus }) => void;
  'conversation:updated': (data: ConversationDto) => void;
  'conversation:assigned': (data: { conversationId: string; userId: string }) => void;
  'notification:new': (data: NotificationDto) => void;
  'agent:typing': (data: { conversationId: string; userId: string }) => void;
  'user:online': (data: { userId: string; online: boolean }) => void;
}

// Eventos del cliente → servidor
interface ClientEvents {
  'conversation:join': (conversationId: string) => void;
  'conversation:leave': (conversationId: string) => void;
  'message:send': (data: SendMessageRequestDto) => void;
  'message:typing': (data: { conversationId: string }) => void;
  'message:stop-typing': (data: { conversationId: string }) => void;
}
```

### 7.3 Provider + Hooks

```typescript
// SocketProvider conecta automáticamente cuando hay token
// useSocketEvent('message:new', handler) se subscribe/desubscribe
// socket.store.ts maneja reconnect con backoff
```

## 8. Autenticación (JWT + Refresh Token)

### 8.1 Flujo

```
1. User sends login (email + password)
2. Backend returns { accessToken (15min), refreshToken (7d), user, tenant }
3. Store tokens in Zustand (memory) + localStorage (persist)
4. Axios interceptor attaches Bearer token to all requests
5. On 401 → interceptor calls /auth/refresh with refresh token
6. If refresh succeeds → retry original request
7. If refresh fails → redirect to /login
```

### 8.2 Seguridad

- AccessToken solo en memoria (Zustand state)
- RefreshToken en localStorage (única persistencia)
- Al recargar página → intentar refresh automático
- Logout → limpiar tokens + desconectar socket

## 9. RBAC (Role Based Access Control)

### 9.1 Jerarquía

```
SUPER_ADMIN > TENANT_ADMIN > SUPERVISOR > AGENT > BOT
```

### 9.2 Permisos

```typescript
// core/rbac/permissions.ts
export const PERMISSIONS = {
  users: {
    view: ['TENANT_ADMIN', 'SUPERVISOR'],
    create: ['TENANT_ADMIN'],
    edit: ['TENANT_ADMIN', 'SUPERVISOR'],
    delete: ['TENANT_ADMIN'],
  },
  conversations: {
    view: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    assign: ['TENANT_ADMIN', 'SUPERVISOR'],
    resolve: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
    close: ['TENANT_ADMIN', 'SUPERVISOR', 'AGENT'],
  },
  // ...
} as const;
```

### 9.3 Uso

```typescript
// <Can I="create" a="user">
//   <Button>Crear Usuario</Button>
// </Can>
```

## 10. Layout System

### 10.1 AuthLayout
```
┌─────────────────────────────────────┐
│          Logo + Brand               │
│                                     │
│      ┌─────────────────────┐        │
│      │   Auth Form         │        │
│      │   (centered card)   │        │
│      └─────────────────────┘        │
│                                     │
│           Footer                    │
└─────────────────────────────────────┘
```

### 10.2 AppLayout
```
┌──────────┬──────────────────────────────────────────┐
│          │  Topbar (search + breadcrumb + actions)   │
│ Sidebar  ├──────────────────────────────────────────┤
│ (nav)    │                                           │
│          │           Main Content                    │
│ collaps. │                                           │
│          │                                           │
│          │                                           │
└──────────┴──────────────────────────────────────────┘
```

### 10.3 ChatLayout (dentro de AppLayout)
```
┌────────────────────────────────────────────────────────────┐
│ Topbar                                                      │
├──────────┬──────────────────────────────────┬───────────────┤
│          │                                  │               │
│   Lista  │    Chat Window                   │ Panel Info    │
│   Conv.  │    (mensajes + input)             │ (contacto)    │
│          │                                  │               │
│          │                                  │               │
├──────────┴──────────────────────────────────┴───────────────┤
│ Footer (opcional)                                           │
└────────────────────────────────────────────────────────────┘
```

## 11. Optimizaciones

| Técnica | Implementación |
|---------|---------------|
| **Code Splitting** | React.lazy + Suspense por módulo/ruta |
| **Virtual List** | TanStack Virtual para listas largas (mensajes) |
| **Debounced Search** | 300ms debounce en búsquedas |
| **Infinite Scroll** | useInfiniteQuery para historial de mensajes |
| **Optimistic Updates** | useMutation.onMutate para enviar mensajes |
| **Prefetching** | queryClient.prefetchQuery en hover |
| **Image Lazy Loading** | loading="lazy" + blur placeholder |
| **Bundle Analysis** | Vite Rollup plugin visualizer |
| **Tree Shaking** | Importaciones directas (lucide, dayjs) |
| **Memo** | React.memo en bubbles, list items |

## 12. Calidad

- **ESLint** flat config con @typescript-eslint/strict
- **Prettier** con reglas consistentes
- **TypeScript strict mode**: noUnusedLocals, noUnusedParameters, exactOptionalPropertyTypes
- **Husky + lint-staged** para pre-commit
- **Componente <ErrorBoundary />** por módulo

## 13. Flujo de Trabajo para Construcción

Cada módulo se construirá en este orden:

1. **Fase 0**: Scaffolding (Vite + configs + dependencias)
2. **Fase 1**: Core (Axios interceptors, stores, providers, layouts)
3. **Fase 2**: Authentication (login, register, refresh, guard)
4. **Fase 3**: Shared Components (atoms → molecules → organisms)
5. **Fase 4**: Dashboard (página principal con stats)
6. **Fase 5**: Chat (módulo más complejo)
7. **Fase 6**: Contacts + Leads
8. **Fase 7**: Products + Categories
9. **Fase 8**: Users + Roles + Permissions
10. **Fase 9**: Settings + Tenant + AI
11. **Fase 10**: Notifications
12. **Fase 11**: Analytics + Reports
13. **Fase 12**: Polish (dark mode, responsive, animations)
