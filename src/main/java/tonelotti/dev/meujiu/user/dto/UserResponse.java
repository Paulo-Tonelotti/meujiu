package tonelotti.dev.meujiu.user.dto;

import tonelotti.dev.meujiu.user.AppUser;
import tonelotti.dev.meujiu.user.enums.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        UUID academyId,
        String academyName
) {
    public UserResponse(AppUser user) {
        this(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getAcademy().getId(),
                user.getAcademy().getName()
        );
    }
}
