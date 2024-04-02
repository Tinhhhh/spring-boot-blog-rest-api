package com.springboot.blog.repository;

import com.springboot.blog.model.entity.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AccessTokenRepository extends JpaRepository<AccessToken, Integer> {

    @Query("""
            select t from AccessToken t inner join User u on t.user.id = u.id
            where u.id = :userId and (t.expired = false or t.revoked = false)
            """)
    List<AccessToken> findALlValidTokensByUser(Long userId);

    AccessToken findByToken(String AccessToken);

}
