package tonelotti.dev.meujiu.common.exceptions;

import java.time.Instant;
import java.util.List;

public record ErrorValidationResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorResponse> errors
) {
    public record FieldErrorResponse(
            String field,
            String message
    ) {}
}
