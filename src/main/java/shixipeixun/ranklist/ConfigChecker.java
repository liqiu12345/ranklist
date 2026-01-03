package shixipeixun.ranklist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
public class ConfigChecker implements CommandLineRunner {

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired(required = false)
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        log.info("\n" + "=".repeat(70));
        log.info("🚀 应用程序配置检查器启动");
        log.info("=".repeat(70));

        // 1. 检查Redis配置
        checkRedisConfig();

        // 2. 检查MySQL配置
        checkMySQLConfig();

        // 3. 检查连接
        testConnections();

        log.info("✅ 配置检查完成");
        log.info("=".repeat(70) + "\n");
    }

    private void checkRedisConfig() {
        log.info("\n[1] Redis配置检查:");
        log.info("  主机: {}", environment.getProperty("spring.data.redis.host", "未设置"));
        log.info("  端口: {}", environment.getProperty("spring.data.redis.port", "未设置"));
        log.info("  数据库: {}", environment.getProperty("spring.data.redis.database", "未设置"));
        log.info("  超时: {}", environment.getProperty("spring.data.redis.timeout", "未设置"));

        String password = environment.getProperty("spring.data.redis.password");
        log.info("  密码: {}", password != null ? "已设置" : "未设置");
    }

    private void checkMySQLConfig() {
        log.info("\n[2] MySQL配置检查:");
        log.info("  URL: {}", environment.getProperty("spring.datasource.url", "未设置"));
        log.info("  用户: {}", environment.getProperty("spring.datasource.username", "未设置"));
        log.info("  驱动: {}", environment.getProperty("spring.datasource.driver-class-name", "未设置"));
    }

    private void testConnections() {
        log.info("\n[3] 连接测试:");

        // 测试Redis连接
        if (redisConnectionFactory != null) {
            try {
                RedisConnection connection = redisConnectionFactory.getConnection();
                String pingResult = connection.ping();
                log.info("  ✅ Redis连接成功: {}", pingResult);

                // 测试数据操作
                String testKey = "config_checker_test";
                connection.stringCommands().set(testKey.getBytes(), "测试成功".getBytes());
                byte[] result = connection.stringCommands().get(testKey.getBytes());
                log.info("  ✅ Redis数据操作测试: {}", new String(result));

                // 删除测试键
                connection.keyCommands().del(testKey.getBytes());
                connection.close();
            } catch (Exception e) {
                log.error("  ❌ Redis连接失败: {}", e.getMessage());
                e.printStackTrace();
            }
        } else {
            log.error("  ❌ RedisConnectionFactory 未注入");
        }

        // 测试MySQL连接
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection()) {
                log.info("  ✅ MySQL连接成功");
                log.info("    数据库: {}", conn.getMetaData().getDatabaseProductName());
                log.info("    版本: {}", conn.getMetaData().getDatabaseProductVersion());
            } catch (Exception e) {
                log.error("  ❌ MySQL连接失败: {}", e.getMessage());
                e.printStackTrace();
            }
        } else {
            log.error("  ❌ DataSource 未注入");
        }
    }
}