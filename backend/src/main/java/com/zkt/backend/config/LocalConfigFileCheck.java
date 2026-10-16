package com.zkt.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;

/**
 * The local profile reads the developer's own (git-ignored) application-local.yml. Without it there is no
 * datasource URL and Spring Boot would silently fall back to a throw-away in-memory H2 database, so startup
 * fails early here with instructions instead.
 */
public class LocalConfigFileCheck implements EnvironmentPostProcessor, Ordered {
    static final String FILE = "application-local.yml";
    static final String MESSAGE = """

            ==================================================================
             缺少本地配置文件 %s，无法连接数据库。
             请在 backend/src/main/resources 目录下执行：
               Copy-Item application-local.yml.example application-local.yml
             然后把其中的 MySQL 账号密码改成你自己的，重新构建后再启动。
             不想装 MySQL 时，可改用 H2 环境：-Dspring-boot.run.profiles=h2
            ==================================================================
            """.formatted(FILE);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        check(environment, new ClassPathResource(FILE).exists());
    }

    static void check(ConfigurableEnvironment environment, boolean localFileExists) {
        if (!localFileExists && environment.acceptsProfiles(Profiles.of("local"))) throw new IllegalStateException(MESSAGE);
    }

    // Runs after config data (application*.yml) is loaded, so default/active profiles are already resolved.
    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
