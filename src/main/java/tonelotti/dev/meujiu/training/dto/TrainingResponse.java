package tonelotti.dev.meujiu.training.dto;

import tonelotti.dev.meujiu.training.TrainingSession;
import tonelotti.dev.meujiu.training.enums.TrainingType;

import java.time.LocalDate;
import java.util.UUID;

public record TrainingResponse(
        UUID id,
        TrainingType type,
        LocalDate trainedOn,
        int durationMinutes,
        String learnedNotes
) {
    public TrainingResponse(TrainingSession trainingSession) {
        this(
                trainingSession.getId(),
                trainingSession.getType(),
                trainingSession.getTrainedOn(),
                trainingSession.getDurationMinutes(),
                trainingSession.getLearnedNotes()
        );
    }
}
