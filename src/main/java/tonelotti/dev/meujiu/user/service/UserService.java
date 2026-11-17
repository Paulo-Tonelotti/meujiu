package tonelotti.dev.meujiu.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tonelotti.dev.meujiu.common.CurrentUser;
import tonelotti.dev.meujiu.common.exceptions.EmailAlreadyUsedException;
import tonelotti.dev.meujiu.common.exceptions.ResourceNotFoundException;
import tonelotti.dev.meujiu.common.helpers.HelperEmail;
import tonelotti.dev.meujiu.user.AppUser;
import tonelotti.dev.meujiu.user.dto.UpdateProfileRequest;
import tonelotti.dev.meujiu.user.dto.UserResponse;
import tonelotti.dev.meujiu.user.repository.UserRepository;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUser currentUser;


    public UserService(UserRepository userRepository, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public UserResponse findById() {

        AppUser user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserResponse(user);
    }

    @Transactional
    public UserResponse update(UpdateProfileRequest request) {

        AppUser user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String email = HelperEmail.normalizeEmail(request.email());

        if(!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)){
            throw new EmailAlreadyUsedException("This email already in use");
        }

        user.setName(request.name());
        user.setEmail(email);

        return new UserResponse(user);
    }

    public AppUser getEntityById(UUID id) {
        return userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
