#!/bin/sh
set -e

# Start nginx in background
nginx -g "daemon off;" &

# Run the Spring Boot application
exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /app/app.jar
