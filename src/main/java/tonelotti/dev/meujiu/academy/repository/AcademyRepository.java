package tonelotti.dev.meujiu.academy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tonelotti.dev.meujiu.academy.Academy;

import java.util.UUID;

public interface AcademyRepository extends JpaRepository<Academy, UUID> {
}
