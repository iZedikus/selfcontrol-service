package ru.stepanov.selfcontrol.simulacrum;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SimulacrumApiLogRepository extends JpaRepository<SimulacrumApiLogEntry, UUID>, JpaSpecificationExecutor<SimulacrumApiLogEntry> {
}
