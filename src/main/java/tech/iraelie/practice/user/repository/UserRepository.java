package tech.iraelie.practice.user.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.iraelie.practice.user.model.User;

import java.util.Optional;

@Repository
@NullMarked
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findAll(Pageable pageable);

    // Single JOIN query instead of user fetch + lazy orders fetch
    @EntityGraph(attributePaths = {"orders"})
    Optional<User> findWithOrdersById(String id);
}