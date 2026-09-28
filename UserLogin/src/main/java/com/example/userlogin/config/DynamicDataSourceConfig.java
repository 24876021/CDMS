package com.example.userlogin.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.example.userlogin.utils.DatabaseInitializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 动态数据源配置（自动切换MySQL/SQLite）
 */
@Configuration
public class DynamicDataSourceConfig {

    // MySQL配置
    @Value("${spring.datasource.url}")
    private String mysqlUrl;

    @Value("${spring.datasource.username}")
    private String mysqlUsername;

    @Value("${spring.datasource.password}")
    private String mysqlPassword;

    @Value("${spring.datasource.driver-class-name}")
    private String mysqlDriver;

    // SQLite配置（直接使用DatabaseInitializer解析后的路径）
    @Value("${spring.datasource.sqlite.driver-class-name}")
    private String sqliteDriver;

    // 添加日志
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DynamicDataSourceConfig.class);

    /**
     * 动态创建数据源（优先MySQL，失败则使用SQLite）
     */
    @Bean
    @Primary // 标记为默认数据源
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();

        // 先尝试配置MySQL数据源
        try {
            // 测试MySQL连接
            DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword);

            // MySQL可用，配置MySQL数据源
            dataSource.setDriverClassName(mysqlDriver);
            dataSource.setUrl(mysqlUrl);
            dataSource.setUsername(mysqlUsername);
            dataSource.setPassword(mysqlPassword);
            logger.info("使用MySQL数据源");
        } catch (SQLException e) {
            // MySQL不可用，配置SQLite数据源
            logger.info("MySQL连接失败，切换到SQLite数据源");

            // 关键：直接使用DatabaseInitializer解析后的URL，避免重复解析
            String sqliteUrl = DatabaseInitializer.getCurrentDbUrl();

            dataSource.setDriverClassName(sqliteDriver);
            dataSource.setUrl(sqliteUrl);
            // SQLite不需要用户名密码
            dataSource.setUsername("");
            dataSource.setPassword("");

            logger.info("SQLite最终连接URL：{}", sqliteUrl);
        }

        // Druid连接池配置
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);

        return dataSource;
    }
}