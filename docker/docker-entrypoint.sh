#!/bin/sh
set -e

# Convertir DATASOURCE_URL de Render (postgres://...) al formato JDBC
if [ -n "$DATASOURCE_URL" ] && [ "${DATASOURCE_URL#jdbc:}" = "$DATASOURCE_URL" ]; then
    DATASOURCE_URL=$(echo "$DATASOURCE_URL" | sed 's|^postgres://|jdbc:postgresql://|; s|^postgresql://|jdbc:postgresql://|')
    export DATASOURCE_URL
fi

# Start nginx in background
nginx -g "daemon off;" &

# Run the Spring Boot application (puerto fijo 8085: nginx usa el 80/PORT)
exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=8085 -jar /app/app.jar
