package com.hamidur.ss.auth.repos;

import com.hamidur.ss.auth.models.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.util.Set;

@Repository
public interface UserRepository extends CrudRepository<User, Integer>
{
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value =
                    "insert into users (first_name, last_name, username, password, enabled) values ((:fn), (:ln), (:un), (:p), (:e));" +
                    "insert into users_roles (user_id, role_id) " +
                            "values (select user_id from users where username = (:un), " +
                            " (select role_id from roles where role = 'USER'))")
    int signUpWithUserRole(@Param("fn") String firstName, @Param("ln") String lastName, @Param("un") String username,
                           @Param("p") String password, @Param("e") boolean enabled);

    @Query(nativeQuery = true, value = "SELECT * FROM users WHERE username = :username")
    User getUserByUsername(@Param("username") String username);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "delete from users_roles where user_id = :u_i and role_id = :r_i")
    int revokeRole(@Param("u_i") Integer userId, @Param("r_i") Integer roleId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "insert into users_roles (user_id, role_id) values (:u_i, :r_i)")
    int addRole(@Param("u_i") Integer userId, @Param("r_i") Integer roleId);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "delete from users_roles where user_id = (:id); delete from users where user_id = (:id)")
    int deleteUserById(@Param("id")Integer userId);

    @Query(nativeQuery = true, value = "select author_id from authors where user_id = :id")
    Integer isUserAnAuthor(@Param("id")Integer userId);

    @Transactional
    @Modifying
    @Query(nativeQuery = true,
    value = "delete from users_roles where user_id = (:u_id);" +
            "delete from authors_articles aa where aa.author_id = (:a_id);" +
            "delete from authors a where a.author_id = (:a_id);"+
            "delete from users where user_id = (:u_id);")
    int deleteAllInfoByUserId(@Param("a_id")Integer authorId, @Param("u_id")Integer userId);

    @Query(nativeQuery = true, value =
            "select * from users where user_id in " +
            "(select user_id from users_roles ur inner join roles r on ur.role_id = r.role_id and r.role = 'AUTHOR');")
    Set<User> getAllAuthors();

    @Query(nativeQuery = true, value =
            "select * from users u inner join users_roles ur on u.user_id = ur.user_id" +
            " and u.user_id = (:userId) and ur.role_id = (select role_id from roles where role = 'AUTHOR')")
    User getAuthorByUserId(@Param("userId") Integer userId);
}
