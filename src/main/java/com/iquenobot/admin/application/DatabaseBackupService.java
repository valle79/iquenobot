package com.iquenobot.admin.application;

import com.iquenobot.admin.domain.dto.BackupInfoDto;
import com.iquenobot.shared.application.SystemSettingsService;
import com.iquenobot.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Servicio de respaldos lógicos de la base de datos.
 * Genera un archivo SQL autocontenido (DDL + INSERTs) leyendo el esquema y los
 * datos a través de JDBC, por lo que no requiere pg_dump ni acceso al filesystem
 * del servidor de base de datos (compatible con Neon y cualquier PostgreSQL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseBackupService {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final DataSource dataSource;
    private final SystemSettingsService settingsService;

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    public List<BackupInfoDto> listBackups() {
        Path dir = backupsDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        List<BackupInfoDto> backups = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "iquenobot-backup-*.sql")) {
            for (Path path : stream) {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                backups.add(BackupInfoDto.builder()
                        .fileName(path.getFileName().toString())
                        .sizeBytes(attrs.size())
                        .createdAt(attrs.creationTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                        .build());
            }
        } catch (IOException e) {
            log.warn("Could not list backups: {}", e.getMessage());
        }

        backups.sort(Comparator.comparing(BackupInfoDto::getCreatedAt).reversed());
        return backups;
    }

    public BackupInfoDto createBackup() {
        Path dir = backupsDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("No se pudo crear el directorio de respaldos: " + dir);
        }

        String fileName = "iquenobot-backup-" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".sql";
        Path target = dir.resolve(fileName);

        try (Connection connection = dataSource.getConnection();
             BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {

            String schema = resolveSchema(connection);
            writer.write("-- IquenoBot backup");
            writer.newLine();
            writer.write("-- Generado: " + LocalDateTime.now());
            writer.newLine();
            writer.write("-- Base de datos: " + connection.getCatalog());
            writer.newLine();
            writer.newLine();
            writer.write("BEGIN;");
            writer.newLine();
            writer.newLine();

            List<String> tables = listTables(connection, schema);
            for (String table : tables) {
                writeTable(writer, connection, schema, table);
            }

            writer.write("COMMIT;");
            writer.newLine();
        } catch (IOException | SQLException e) {
            log.error("Backup failed: {}", e.getMessage(), e);
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // best effort
            }
            throw new BusinessException("Error al crear el respaldo: " + e.getMessage());
        }

        enforceRetention();

        log.info("Backup created: {}", fileName);
        return BackupInfoDto.builder()
                .fileName(fileName)
                .sizeBytes(safeSize(target))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void writeTable(BufferedWriter writer, Connection connection, String schema, String table)
            throws IOException, SQLException {
        String qualified = quote(schema) + "." + quote(table);

        writer.write("-- ============================================");
        writer.newLine();
        writer.write("-- Tabla: " + table);
        writer.newLine();
        writer.write("-- ============================================");
        writer.newLine();
        writer.newLine();

        writeCreateTable(writer, connection, schema, table, qualified);
        writeInsertStatements(writer, connection, qualified, table);
        writer.newLine();
    }

    private void writeCreateTable(BufferedWriter writer, Connection connection, String schema, String table, String qualified)
            throws IOException, SQLException {
        try (ResultSet rs = connection.getMetaData().getColumns(connection.getCatalog(), schema, table, "%")) {
            List<String> columnDefs = new ArrayList<>();
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String typeName = rs.getString("TYPE_NAME");
                int size = rs.getInt("COLUMN_SIZE");
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                String defaultValue = rs.getString("COLUMN_DEF");

                StringBuilder def = new StringBuilder();
                def.append(quote(name)).append(' ').append(mapType(typeName, size, decimalDigits));
                if (defaultValue != null && !defaultValue.isBlank()) {
                    def.append(" DEFAULT ").append(defaultValue);
                }
                if (!nullable) {
                    def.append(" NOT NULL");
                }
                columnDefs.add(def.toString());
            }

            writer.write("CREATE TABLE IF NOT EXISTS " + qualified + " (");
            writer.newLine();
            writer.write("    " + String.join("," + System.lineSeparator() + "    ", columnDefs));
            writer.newLine();
            writer.write(");");
            writer.newLine();
            writer.newLine();
        }
    }

    private void writeInsertStatements(BufferedWriter writer, Connection connection, String qualified, String table)
            throws IOException, SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + qualified);
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(quote(meta.getColumnLabel(i)));
            }
            String colList = String.join(", ", columns);

            int rows = 0;
            while (rs.next()) {
                List<String> values = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    values.add(formatValue(rs, i, meta.getColumnType(i)));
                }
                writer.write("INSERT INTO " + qualified + " (" + colList + ") VALUES ("
                        + String.join(", ", values) + ");");
                writer.newLine();
                rows++;
            }
            log.debug("Table {}: {} rows backed up", table, rows);
        }
    }

    private String formatValue(ResultSet rs, int index, int type) throws SQLException {
        Object value = rs.getObject(index);
        if (value == null) {
            return "NULL";
        }

        return switch (type) {
            case Types.BOOLEAN, Types.BIT -> ((Boolean) value) ? "TRUE" : "FALSE";
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT, Types.NUMERIC, Types.DECIMAL,
                    Types.FLOAT, Types.REAL, Types.DOUBLE -> value.toString();
            case Types.DATE -> "'" + rs.getDate(index).toLocalDate() + "'";
            case Types.TIME -> "'" + rs.getTime(index).toLocalTime() + "'";
            case Types.TIMESTAMP -> "'" + rs.getTimestamp(index).toLocalDateTime() + "'";
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> "NULL /* binary data omitted */";
            default -> "'" + escapeSql(String.valueOf(value)) + "'";
        };
    }

    private String escapeSql(String value) {
        return value.replace("'", "''").replace("\\", "\\\\");
    }

    private String mapType(String typeName, int size, int decimalDigits) {
        String upper = typeName.toUpperCase();
        return switch (upper) {
            case "INT4", "INTEGER" -> "INTEGER";
            case "INT8", "BIGINT" -> "BIGINT";
            case "INT2", "SMALLINT" -> "SMALLINT";
            case "BOOL", "BOOLEAN" -> "BOOLEAN";
            case "FLOAT4", "REAL" -> "REAL";
            case "FLOAT8", "DOUBLE PRECISION" -> "DOUBLE PRECISION";
            case "NUMERIC", "DECIMAL" -> decimalDigits > 0 ? "NUMERIC(" + size + "," + decimalDigits + ")" : "NUMERIC";
            case "VARCHAR", "BPCHAR", "CHAR", "CHARACTER VARYING" -> size > 0 && size < Integer.MAX_VALUE ? "VARCHAR(" + size + ")" : "TEXT";
            case "TEXT" -> "TEXT";
            case "DATE" -> "DATE";
            case "TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE" -> "TIMESTAMP";
            case "TIMESTAMPTZ", "TIMESTAMP WITH TIME ZONE" -> "TIMESTAMPTZ";
            case "TIME" -> "TIME";
            case "UUID" -> "UUID";
            case "JSON", "JSONB" -> "JSONB";
            case "BYTEA" -> "BYTEA";
            default -> "TEXT";
        };
    }

    private List<String> listTables(Connection connection, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    private String resolveSchema(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT current_schema()");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "public";
    }

    private void enforceRetention() {
        int retentionDays = settingsService.getInt("backups", "backup_retention", 7);
        if (retentionDays <= 0) {
            return;
        }

        Path dir = backupsDir();
        if (!Files.isDirectory(dir)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "iquenobot-backup-*.sql")) {
            for (Path path : stream) {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                LocalDateTime createdAt = attrs.creationTime().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                if (createdAt.isBefore(LocalDateTime.now().minusDays(retentionDays))) {
                    Files.deleteIfExists(path);
                    log.info("Deleted old backup: {}", path.getFileName());
                }
            }
        } catch (IOException e) {
            log.warn("Could not enforce backup retention: {}", e.getMessage());
        }
    }

    private Path backupsDir() {
        return Paths.get(uploadPath).toAbsolutePath().normalize().resolve("backups");
    }

    private String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }
}
