package tonelotti.dev.meujiu.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tonelotti.dev.meujiu.academy.Academy;
import tonelotti.dev.meujiu.academy.service.AcademyService;
import tonelotti.dev.meujiu.auth.dto.RegisterRequest;
import tonelotti.dev.meujiu.common.exceptions.EmailAlreadyUsedException;
import tonelotti.dev.meujiu.common.helpers.HelperEmail;
import tonelotti.dev.meujiu.user.AppUser;
import tonelotti.dev.meujiu.user.dto.UserResponse;
import tonelotti.dev.meujiu.user.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AcademyService academyService;
    private final PasswordEncoder passwordEncoder;


    public AuthService(UserRepository userRepository,
                       AcademyService academyService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.academyService = academyService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(RegisterRequest request) {
        Academy academy = academyService.findById(request.academyId());
        String email = HelperEmail.normalizeEmail(request.email());

        boolean userAlreadyExistsWithEmail = userRepository.existsByEmail(email);

        if(userAlreadyExistsWithEmail){
            throw new EmailAlreadyUsedException("This email already in use");
        }

        AppUser user = new AppUser(
                academy,
                request.name(),
                email,
                passwordEncoder.encode(request.password())
        );

        return new UserResponse(userRepository.save(user));

    }
}
