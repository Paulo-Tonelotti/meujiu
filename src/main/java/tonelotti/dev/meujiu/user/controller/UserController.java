package tonelotti.dev.meujiu.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tonelotti.dev.meujiu.user.dto.UpdateProfileRequest;
import tonelotti.dev.meujiu.user.dto.UserResponse;
import tonelotti.dev.meujiu.user.service.UserService;

@RestController
@RequestMapping("/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> showProfile() {
        return ResponseEntity.ok(userService.findById());
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.update(request));
    }
}
