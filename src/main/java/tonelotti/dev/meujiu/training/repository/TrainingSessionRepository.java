package tonelotti.dev.meujiu.training.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tonelotti.dev.meujiu.training.TrainingSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, UUID> {
    List<TrainingSession> findByUserIdOrderByTrainedOnDesc(UUID userId);

    Optional<TrainingSession> findByIdAndUserId(UUID id, UUID userId);

    List<TrainingSession> findByUserIdAndTrainedOnBetweenOrderByTrainedOnDesc(UUID userId, LocalDate from, LocalDate to);
}
