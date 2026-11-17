package tonelotti.dev.meujiu.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import tonelotti.dev.meujiu.common.exceptions.UnauthorizedException;

import java.util.UUID;

@Component
public class CurrentUser {

    private final HttpServletRequest request;

    public CurrentUser(HttpServletRequest request) {
        this.request = request;
    }

    public UUID id() {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new UnauthorizedException("Missing X-User-Id header");
        }
        try {
            return UUID.fromString(header);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid X-User-Id header");
        }
    }
}