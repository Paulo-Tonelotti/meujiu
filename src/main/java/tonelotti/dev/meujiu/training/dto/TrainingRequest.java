package tonelotti.dev.meujiu.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import tonelotti.dev.meujiu.training.enums.TrainingType;

import java.time.LocalDate;

public record TrainingRequest(
        @NotNull(message = "Type is required")
        TrainingType type,

        @NotNull(message = "Date is required")
        @PastOrPresent(message = "Date cannot be in the future")
        LocalDate trainedOn,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Max(value = 600, message = "Duration cannot exceed 600 minutes")
        Integer durationMinutes,

        String learnedNotes
) {}