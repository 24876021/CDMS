package com.example.userlogin.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库初始化工具类（支持MySQL/SQLite自动降级）
 * 核心功能：创建数据库 + 执行建表脚本（无需监听器，在数据源初始化时调用）
 * 新增功能：检测表是否存在，若所有表都已存在，则跳过建表脚本执行
 */
public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    // 配置文件路径
    private static final String CONFIG_FILE = "application.yaml";
    // MySQL配置项
    private static String MYSQL_TEMP_URL;
    private static String MYSQL_USERNAME;
    private static String MYSQL_PASSWORD;
    private static String DB_NAME;
    // SQLite配置项
    private static String SQLITE_URL;
    private static String SQLITE_SCHEMA_PATH;
    private static String SQLITE_FILE_PATH; // 最终的文件路径
    // MySQL建表脚本路径（固定）
    private static final String MYSQL_SCHEMA_PATH = "classpath:sql/schema-tables-mysql.sql";

    // 防止重复初始化
    private static volatile boolean initialized = false;

    // 静态代码块加载配置
    static {
        try {
            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            ClassPathResource resource = new ClassPathResource(CONFIG_FILE);
            List<PropertySource<?>> propertySources = loader.load("yamlConfig", resource);

            if (propertySources.isEmpty()) {
                throw new RuntimeException("未加载到任何YAML配置");
            }

            PropertySource<?> propertySource = propertySources.get(0);
            // 1. 先加载数据库名称（核心）
            DB_NAME = (String) propertySource.getProperty("database.init.name");
            if (DB_NAME == null || DB_NAME.isEmpty()) {
                throw new RuntimeException("数据库名称配置缺失！");
            }

            // 2. 加载MySQL核心配置
            MYSQL_TEMP_URL = (String) propertySource.getProperty("database.init.temp-url");
            MYSQL_USERNAME = (String) propertySource.getProperty("database.init.username");
            MYSQL_PASSWORD = (String) propertySource.getProperty("database.init.password");

            // 3. 加载并解析SQLite路径（关键：先替换占位符）
            String sqlitePathTemplate = (String) propertySource.getProperty("database.init.sqlite.path");
            if (sqlitePathTemplate == null || sqlitePathTemplate.isEmpty()) {
                throw new RuntimeException("SQLite路径配置缺失！");
            }
            // 替换路径中的 ${database.init.name} 为实际数据库名
            SQLITE_FILE_PATH = sqlitePathTemplate.replace("${database.init.name}", DB_NAME);

            // 4. 构建SQLite的JDBC URL
            String sqliteFullUrlTemplate = (String) propertySource.getProperty("database.init.sqlite-full-url");
            SQLITE_URL = sqliteFullUrlTemplate.replace("${database.init.name}", DB_NAME);

            // 5. 加载SQLite脚本路径
            SQLITE_SCHEMA_PATH = (String) propertySource.getProperty("database.init.sqlite.schema-location");

            // 校验核心配置
            if (MYSQL_TEMP_URL == null || MYSQL_USERNAME == null) {
                throw new RuntimeException("MySQL连接配置缺失！");
            }

            // 打印解析结果（调试用）
            logger.info("配置加载成功 ===>");
            logger.info("  数据库名称: {}", DB_NAME);
            logger.info("  SQLite文件路径: {}", SQLITE_FILE_PATH);
            logger.info("  SQLite JDBC URL: {}", SQLITE_URL);

        } catch (Exception e) {
            throw new RuntimeException("加载数据库配置失败，项目无法启动", e);
        }
    }

    /**
     * 对外暴露的初始化入口（核心方法）
     * 优先初始化MySQL，失败则自动降级到SQLite
     */
    public static void initDatabase() {
        // 双重检查锁，避免重复执行
        if (initialized) {
            logger.info("数据库已初始化，跳过重复执行");
            return;
        }
        synchronized (DatabaseInitializer.class) {
            if (initialized) {
                return;
            }
            try {
                // 尝试初始化MySQL数据库（建库 + 建表）
                initMysqlDatabase();
                logger.info("MySQL数据库初始化成功（包含建库和建表）");
            } catch (Exception e) {
                logger.error("MySQL连接/初始化失败，自动降级到SQLite：{}", e.getMessage(), e);
                // 降级到SQLite
                initSqliteDatabase();
                logger.info("SQLite数据库初始化成功");
            }
            initialized = true;
        }
    }

    /**
     * 初始化MySQL数据库（逻辑不变）
     */
    private static void initMysqlDatabase() {
        // 拼接正确的业务库URL
        String baseMysqlUrl = MYSQL_TEMP_URL.split("\\?")[0];
        String mysqlDbBaseUrl = baseMysqlUrl + DB_NAME;
        String params = MYSQL_TEMP_URL.contains("?") ? MYSQL_TEMP_URL.split("\\?")[1] : "";
        String mysqlDbUrl = mysqlDbBaseUrl + (params.isEmpty() ? "" : "?" + params);

        Connection tempConn = null;
        Statement tempStmt = null;
        Connection dbConn = null;
        Statement dbStmt = null;

        try {
            // 注册MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 1. 连接MySQL服务器，创建数据库（使用temp URL）
            tempConn = DriverManager.getConnection(MYSQL_TEMP_URL, MYSQL_USERNAME, MYSQL_PASSWORD);
            tempStmt = tempConn.createStatement();
            String createDbSql = String.format(
                    "CREATE DATABASE IF NOT EXISTS %s DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci",
                    DB_NAME
            );
            tempStmt.execute(createDbSql);
            logger.info("MySQL数据库 {} 创建/检查完成", DB_NAME);

            // 2. 关闭临时连接，重新连接到业务库
            closeResource(tempStmt, tempConn);

            // 3. 连接到新建的数据库
            logger.info("尝试连接MySQL业务库，URL：{}", mysqlDbUrl);
            dbConn = DriverManager.getConnection(mysqlDbUrl, MYSQL_USERNAME, MYSQL_PASSWORD);
            dbStmt = dbConn.createStatement();
            dbStmt.setQueryTimeout(30);

            // 4. 读取建表脚本
            Resource schemaResource = new ClassPathResource(MYSQL_SCHEMA_PATH.replace("classpath:", ""));
            if (!schemaResource.exists()) {
                throw new RuntimeException("MySQL建表脚本不存在：" + MYSQL_SCHEMA_PATH);
            }
            logger.info("开始读取MySQL建表脚本：{}", MYSQL_SCHEMA_PATH);
            String sqlScript = new String(FileCopyUtils.copyToByteArray(schemaResource.getInputStream()), StandardCharsets.UTF_8);

            // 5. 检查表是否已全部存在
            Set<String> existingTables = getExistingTableNames(dbConn, DB_NAME);
            Set<String> tablesInScript = extractTableNamesFromSqlScript(sqlScript);
            if (!tablesInScript.isEmpty() && existingTables.containsAll(tablesInScript)) {
                logger.info("MySQL数据库中已存在所有建表脚本定义的表，跳过建表脚本执行");
                return; // 表已存在，跳过建表
            }

            // 6. 执行建表脚本
            List<String> sqlStatements = splitSqlScript(sqlScript);
            logger.info("成功读取MySQL脚本，共解析出 {} 条SQL语句", sqlStatements.size());

            int successCount = 0;
            int skipCount = 0;
            for (int i = 0; i < sqlStatements.size(); i++) {
                String sql = sqlStatements.get(i).trim();
                if (sql.isEmpty() || sql.startsWith("--")) {
                    skipCount++;
                    continue;
                }

                try {
                    boolean isQuery = sql.trim().toUpperCase().startsWith("SELECT");
                    if (isQuery) {
                        dbStmt.executeQuery(sql);
                    } else {
                        dbStmt.executeUpdate(sql);
                    }
                    successCount++;
                    logger.debug("执行MySQL SQL成功 [{}]: {}", successCount,
                            sql.substring(0, Math.min(sql.length(), 80)) + (sql.length() > 80 ? "..." : ""));
                } catch (Exception e) {
                    String msg = e.getMessage().toLowerCase();
                    if (msg.contains("already exists") || msg.contains("duplicate entry") || msg.contains("unique constraint")) {
                        skipCount++;
                        logger.warn("跳过重复SQL [{}]: {}", i+1, sql.substring(0, Math.min(sql.length(), 50)));
                        continue;
                    }
                    throw new RuntimeException("执行第 " + (i+1) + " 条SQL失败: " + sql, e);
                }
            }

            logger.info("MySQL建表脚本执行完成 - 成功: {} 条, 跳过: {} 条", successCount, skipCount);

        } catch (Exception e) {
            logger.error("MySQL建库/建表失败", e);
            throw new RuntimeException("MySQL建库/建表失败: " + e.getMessage(), e);
        } finally {
            closeResource(dbStmt, dbConn);
            closeResource(tempStmt, tempConn);
        }
    }

    /**
     * 初始化SQLite数据库（修复路径创建逻辑）
     */
    private static void initSqliteDatabase() {
        try {
            // 切换SQL初始化平台为sqlite
            System.setProperty("spring.sql.init.platform", "sqlite");
            // 注册SQLite驱动
            Class.forName("org.sqlite.JDBC");

            // 1. 创建存储目录（如果不存在）
            File sqliteFile = new File(SQLITE_FILE_PATH);
            File parentDir = sqliteFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean dirCreated = parentDir.mkdirs();
                if (dirCreated) {
                    logger.info("创建SQLite存储目录：{}", parentDir.getAbsolutePath());
                } else {
                    throw new RuntimeException("无法创建SQLite存储目录：" + parentDir.getAbsolutePath());
                }
            }

            // 2. 创建SQLite数据库文件（不存在则自动创建）
            if (!sqliteFile.exists()) {
                boolean fileCreated = sqliteFile.createNewFile();
                if (fileCreated) {
                    logger.info("创建SQLite数据库文件：{}", sqliteFile.getAbsolutePath());
                } else {
                    throw new RuntimeException("无法创建SQLite数据库文件：" + sqliteFile.getAbsolutePath());
                }
            } else {
                logger.info("SQLite数据库文件已存在：{}", sqliteFile.getAbsolutePath());
            }

            // 3. 执行PRAGMA配置（事务外执行）
            try (Connection conn = DriverManager.getConnection(SQLITE_URL);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
                stmt.execute("PRAGMA journal_mode = WAL");
                stmt.execute("PRAGMA synchronous = NORMAL");
                stmt.execute("PRAGMA cache_size = -10240");
                logger.info("SQLite PRAGMA配置执行完成");
            }

            // 4. 读取建表脚本
            Resource schemaResource = new ClassPathResource(SQLITE_SCHEMA_PATH.replace("classpath:", ""));
            if (!schemaResource.exists()) {
                throw new RuntimeException("SQLite建表脚本不存在：" + SQLITE_SCHEMA_PATH);
            }
            String sqlScript = new String(FileCopyUtils.copyToByteArray(schemaResource.getInputStream()), StandardCharsets.UTF_8);

            // 5. 检查表是否已全部存在
            try (Connection conn = DriverManager.getConnection(SQLITE_URL)) {
                Set<String> existingTables = getExistingTableNames(conn);
                Set<String> tablesInScript = extractTableNamesFromSqlScript(sqlScript);
                if (!tablesInScript.isEmpty() && existingTables.containsAll(tablesInScript)) {
                    logger.info("SQLite数据库中已存在所有建表脚本定义的表，跳过建表脚本执行");
                    return; // 表已存在，跳过建表
                }
            }

            // 6. 执行建表脚本（事务内执行）
            try (Connection conn = DriverManager.getConnection(SQLITE_URL);
                 Statement stmt = conn.createStatement()) {

                conn.setAutoCommit(false);
                List<String> sqlStatements = splitSqlScript(sqlScript);

                int successCount = 0;
                int skipCount = 0;
                for (String sql : sqlStatements) {
                    sql = sql.trim();
                    if (sql.isEmpty() || sql.startsWith("--") || sql.startsWith("PRAGMA")) {
                        skipCount++;
                        continue;
                    }
                    try {
                        stmt.execute(sql);
                        successCount++;
                        logger.debug("执行SQLite SQL成功 [{}]: {}", successCount,
                                sql.substring(0, Math.min(sql.length(), 80)) + (sql.length() > 80 ? "..." : ""));
                    } catch (Exception e) {
                        if (e.getMessage().contains("UNIQUE constraint failed") ||
                                e.getMessage().contains("already exists")) {
                            skipCount++;
                            logger.warn("SQLite SQL执行跳过（数据已存在）: {}", sql.substring(0, Math.min(sql.length(), 50)));
                            continue;
                        }
                        throw e;
                    }
                }

                conn.commit();
                logger.info("SQLite建表脚本执行完成 - 成功: {} 条, 跳过: {} 条", successCount, skipCount);
            }

        } catch (Exception e) {
            logger.error("SQLite数据库初始化失败", e);
            throw new RuntimeException("SQLite数据库初始化失败", e);
        }
    }

    /**
     * 获取MySQL数据库中所有表名
     */
    private static Set<String> getExistingTableNames(Connection conn, String databaseName) throws Exception {
        Set<String> tableNames = new HashSet<>();
        String sql = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, databaseName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableNames.add(rs.getString(1).toLowerCase());
                }
            }
        }
        return tableNames;
    }

    /**
     * 获取SQLite数据库中所有表名
     */
    private static Set<String> getExistingTableNames(Connection conn) throws Exception {
        Set<String> tableNames = new HashSet<>();
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tableNames.add(rs.getString(1).toLowerCase());
            }
        }
        return tableNames;
    }

    /**
     * 从SQL脚本中提取所有CREATE TABLE语句中的表名
     */
    private static Set<String> extractTableNamesFromSqlScript(String sqlScript) {
        Set<String> tableNames = new HashSet<>();
        // 匹配 CREATE TABLE [IF NOT EXISTS] `表名` 或 表名 (忽略大小写)
        Pattern pattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[`\"']?([a-zA-Z0-9_]+)[`\"']?",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sqlScript);
        while (matcher.find()) {
            tableNames.add(matcher.group(1).toLowerCase());
        }
        return tableNames;
    }

    /**
     * 安全分割SQL脚本（解决注释/字符串中的分号误分割问题）
     */
    private static List<String> splitSqlScript(String sqlContent) {
        List<String> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inComment = false;

        for (char c : sqlContent.toCharArray()) {
            // 处理行注释
            if (inComment) {
                if (c == '\n') {
                    inComment = false;
                }
                continue;
            }

            // 处理单/双引号（避免分割字符串内的分号）
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }

            // 检测行注释起始（--）
            if (c == '-' && currentStatement.length() > 0 &&
                    currentStatement.charAt(currentStatement.length() - 1) == '-') {
                inComment = true;
                currentStatement.setLength(currentStatement.length() - 1); // 移除最后一个-
                continue;
            }

            // 仅在非引号/非注释状态下分割分号
            if (c == ';' && !inSingleQuote && !inDoubleQuote && !inComment) {
                statements.add(currentStatement.toString().trim());
                currentStatement.setLength(0);
            } else {
                currentStatement.append(c);
            }
        }

        // 添加最后一条未以分号结尾的语句
        String lastStmt = currentStatement.toString().trim();
        if (!lastStmt.isEmpty()) {
            statements.add(lastStmt);
        }

        return statements;
    }

    /**
     * 关闭数据库资源
     */
    private static void closeResource(Statement stmt, Connection conn) {
        try {
            if (stmt != null) stmt.close();
        } catch (Exception e) {
            logger.warn("关闭Statement失败", e);
        }
        try {
            if (conn != null) conn.close();
        } catch (Exception e) {
            logger.warn("关闭Connection失败", e);
        }
    }

    /**
     * 获取当前使用的数据库URL（供业务代码调用）
     */
    public static String getCurrentDbUrl() {
        // 检查MySQL是否可用
        try (Connection conn = DriverManager.getConnection(MYSQL_TEMP_URL, MYSQL_USERNAME, MYSQL_PASSWORD)) {
            return "jdbc:mysql://localhost:3306/" + DB_NAME +
                    "?serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&useSSL=false";
        } catch (Exception e) {
            return SQLITE_URL;
        }
    }

    /**
     * 获取最终的SQLite文件路径（供外部调用）
     */
    public static String getSqliteFilePath() {
        return SQLITE_FILE_PATH;
    }
}