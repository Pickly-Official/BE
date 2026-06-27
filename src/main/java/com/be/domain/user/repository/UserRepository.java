package com.be.domain.user.repository;

import com.be.domain.user.entity.Provider;
import com.be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
