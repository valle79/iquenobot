#!/bin/sh
set -e

# Convertir DATASOURCE_URL de Render (postgres://...) al formato JDBC.
# El driver JDBC de PostgreSQL no acepta credenciales embebidas en la URL,
# asi que se extraen user/password y se pasan como variables separadas.
if [ -n "$DATASOURCE_URL" ] && [ "${DATASOURCE_URL#jdbc:}" = "$DATASOURCE_URL" ]; then
    REST=$(echo "$DATASOURCE_URL" | sed 's|^postgresql*://||')

    case "$REST" in
        *@*)
            CREDS=$(echo "$REST" | sed 's|@.*||')
            HOSTPORT=$(echo "$REST" | sed 's|^[^@]*@||')
            if [ -z "$DATASOURCE_USERNAME" ]; then
                DATASOURCE_USERNAME=$(echo "$CREDS" | sed 's|:.*||')
            fi
            if [ -z "$DATASOURCE_PASSWORD" ]; then
                DATASOURCE_PASSWORD=$(echo "$CREDS" | sed 's|^[^:]*:||')
            fi
            export DATASOURCE_USERNAME DATASOURCE_PASSWORD
            ;;
        *)
            HOSTPORT=$REST
            ;;
    esac

    DATASOURCE_URL="jdbc:postgresql://$HOSTPORT"
    export DATASOURCE_URL
fi

# Start nginx in background
nginx -g "daemon off;" &

# Run the Spring Boot application (puerto fijo 8085: nginx usa el 80/PORT)
exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=8085 -jar /app/app.jar
