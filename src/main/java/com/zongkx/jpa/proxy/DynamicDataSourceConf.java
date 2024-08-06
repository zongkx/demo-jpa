package com.zongkx.jpa.proxy;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.duckdb.DuckDBDriver;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

@Component
@RequiredArgsConstructor
@AutoConfigureAfter(value = DataSourceAutoConfiguration.class)
public class DynamicDataSourceConf {
    private final JpaProperties jpaProperties;
    private final HibernateProperties hibernateProperties;
    private final DataSource dataSource;
    private DataSource myDataSource;

    @PostConstruct
    public void init() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(DuckDBDriver.class.getCanonicalName());
        config.setMaximumPoolSize(10);
        config.setJdbcUrl("jdbc:duckdb:");
        myDataSource = new HikariDataSource(config);
    }

    @Bean
    public WebMvcConfigurer duckDBInterceptor() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                InterceptorRegistration addInterceptor = registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                        if (handler instanceof HandlerMethod) {
                            Method method = ((HandlerMethod) handler).getMethod();
                            if (method.isAnnotationPresent(DuckDBDataSource.class)) {
                                DuckDBThreadLocal.setRoutingDataSource("duckdb");
                            }
                        }
                        return true;
                    }

                    @Override
                    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                        DuckDBThreadLocal.removeRoutingDataSource();
                    }
                });
                addInterceptor.addPathPatterns("/**");
            }
        };
    }

    @Bean
    public EntityManagerFactory entityManagerFactory() {
        final Map<String, EntityManagerFactory> entityManagerFactoryMap = new HashMap<>();
        final EntityManagerFactory defaultEmf = this.createEntityManagerFactory(H2Dialect.class.getName(), "h2Factory", dataSource);
        entityManagerFactoryMap.put("duckdb", this.createEntityManagerFactory(PostgreSQLDialect.class.getName(), "duckdbFactory", myDataSource));
        entityManagerFactoryMap.put("default", defaultEmf);
        final int hashcode = UUID.randomUUID().toString().hashCode();
        final Object emf = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{EntityManagerFactory.class},
                (proxy, method, args) -> {
                    if ("hashCode".equals(method.getName())) {
                        return hashcode;
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    String routingDataSource1 = DuckDBThreadLocal.getRoutingDataSource();
                    Object instance1 = entityManagerFactoryMap.get(routingDataSource1);
                    if (Objects.nonNull(instance1)) {
                        return method.invoke(instance1);
                    }
                    return method.invoke(defaultEmf);
                }
        );
        return (EntityManagerFactory) emf;
    }

    @Bean
    public JPAQueryFactory queryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

    private EntityManagerFactory createEntityManagerFactory(String dialectClassName, String providerName, DataSource dataSource) {
        final LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        if (Objects.equals(H2Dialect.class.getName(), dialectClassName)) {
            Map<String, String> properties = jpaProperties.getProperties();
            properties.put("hibernate.ddl-auto", hibernateProperties.getDdlAuto());
            properties.put("hibernate.session_factory.statement_inspector", MyJpaInterceptor.class.getName());
            factory.setJpaPropertyMap(properties);
            factory.setPackagesToScan("com.zongkx.*");
            factory.setDataSource(dataSource);
            final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setShowSql(jpaProperties.isShowSql());
            vendorAdapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
            vendorAdapter.setGenerateDdl(true);
            vendorAdapter.setDatabase(Database.H2);
            factory.setJpaVendorAdapter(vendorAdapter);
        } else {
            final Properties properties = new Properties() {{
                put("hibernate.implicit_naming_strategy", new SpringImplicitNamingStrategy());
                put("hibernate.session_factory.statement_inspector", MyJpaInterceptor.class.getName());
                put("hibernate.temp.use_jdbc_metadata_defaults", false);
            }};
            factory.setJpaProperties(properties);
            factory.setPackagesToScan("com.zongkx.*");
            factory.setDataSource(dataSource);
            final HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setShowSql(jpaProperties.isShowSql());
            vendorAdapter.setPrepareConnection(false);
            vendorAdapter.setDatabasePlatform(dialectClassName);
            factory.setJpaVendorAdapter(vendorAdapter);
        }
        factory.afterPropertiesSet();
        factory.setPersistenceUnitName(providerName + "Unit");
        factory.setBeanName(providerName + "Bean");
        return factory.getObject();
    }

}
