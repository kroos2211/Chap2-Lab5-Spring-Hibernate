package com.microservice.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@PropertySource("classpath:application.properties")
public class HibernateConfig {
    private final Environment environment;

    public HibernateConfig(Environment environment) {
        this.environment = environment;
    }

    private String setting(String key) {
        String prefix = "h2".equalsIgnoreCase(environment.getProperty("product.profile")) ? "h2." : "";
        return environment.getRequiredProperty(prefix + key);
    }

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName(setting("jdbc.driver"));
        source.setUrl(setting("jdbc.url"));
        source.setUsername(setting("jdbc.username"));
        if (!"h2".equalsIgnoreCase(environment.getProperty("product.profile"))) {
            source.setPassword(environment.getProperty("PRODUCT_DB_PASSWORD", ""));
        }
        return source;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource) {
        LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.microservice.product.entity");
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", setting("hibernate.dialect"));
        properties.setProperty("hibernate.show_sql", setting("hibernate.show_sql"));
        properties.setProperty("hibernate.hbm2ddl.auto", setting("hibernate.hbm2ddl.auto"));
        factory.setHibernateProperties(properties);
        return factory;
    }
}
