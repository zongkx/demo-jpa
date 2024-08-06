package com.zongkx.jpa;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.sql.SQLExpressions;
import com.zongkx.jpa.dao.UserDao;
import com.zongkx.jpa.entity.QUser;
import com.zongkx.jpa.entity.User;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class DemoJpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoJpaApplication.class, args);
    }


    @Bean()
    public CommandLineRunner demo1(JPAQueryFactory queryFactory) {
        // 子查询 select * from t_user where start = (select min(start) from t_user)
        List<User> fetch = queryFactory.selectFrom(QUser.user)
                .where(QUser.user.start.eq(SQLExpressions.select(QUser.user.start.min()).from(QUser.user)))
                .fetch();
        System.out.println(fetch);
        return args -> {
        };
    }

    @Bean()
    public CommandLineRunner demo2(UserDao userDao) {
        String version = userDao.selectVersion();
        System.out.println(version);
        return args -> {
        };
    }
}
