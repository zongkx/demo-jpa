package com.zongkx.jpa.dao;

import com.zongkx.jpa.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDao extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Query(value = "select version()", nativeQuery = true)
    String selectVersion();
}
