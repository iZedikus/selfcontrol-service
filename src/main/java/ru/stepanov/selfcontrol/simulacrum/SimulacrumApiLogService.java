package ru.stepanov.selfcontrol.simulacrum;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulacrumApiLogService {
    private final SimulacrumApiLogRepository repository;

    public SimulacrumApiLogService(SimulacrumApiLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SimulacrumApiLogEntry record(SimulacrumApiLogEntry entry) {
        return repository.save(entry);
    }
}
