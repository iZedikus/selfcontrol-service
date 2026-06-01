package ru.stepanov.selfcontrol.identity;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String hash);

    @Modifying
    @Query("update RefreshToken t set t.revoked=true where t.user.userId=:userId")
    void revokeAll(@Param("userId") UUID userId);
}
