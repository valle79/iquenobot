package com.iquenobot.shared.enums;

public enum ChatbotFlowTrigger {
    WELCOME,              // Mensaje de bienvenida
    KEYWORD,              // Por palabras clave
    PATTERN,              // Por patrón regex
    INTENT,               // Por intención detectada
    NO_AGENT_AVAILABLE,   // Cuando no hay agentes disponibles
    AFTER_HOURS,          // Fuera de horario
    INACTIVITY,           // Después de inactividad
    MENU,                 // Menú de opciones
    CUSTOM                // Personalizado
}
