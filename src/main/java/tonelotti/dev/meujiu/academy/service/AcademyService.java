package tonelotti.dev.meujiu.academy.service;

import org.springframework.stereotype.Service;
import tonelotti.dev.meujiu.academy.Academy;
import tonelotti.dev.meujiu.academy.repository.AcademyRepository;
import tonelotti.dev.meujiu.common.exceptions.ResourceNotFoundException;

import java.util.UUID;

@Service
public class AcademyService {

    private final AcademyRepository academyRepository;

    public AcademyService(AcademyRepository academyRepository) {
        this.academyRepository = academyRepository;
    }

    public Academy findById(UUID id) {
        return academyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academy Not Found"));
    }
}
