package tonelotti.dev.meujiu.training.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tonelotti.dev.meujiu.common.CurrentUser;
import tonelotti.dev.meujiu.common.exceptions.InvalidPeriodException;
import tonelotti.dev.meujiu.common.exceptions.ResourceNotFoundException;
import tonelotti.dev.meujiu.training.TrainingSession;
import tonelotti.dev.meujiu.training.dto.TrainingRequest;
import tonelotti.dev.meujiu.training.dto.TrainingResponse;
import tonelotti.dev.meujiu.training.repository.TrainingSessionRepository;
import tonelotti.dev.meujiu.user.AppUser;
import tonelotti.dev.meujiu.user.service.UserService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TrainingService {

    private static final LocalDate EARLIEST_DATE = LocalDate.of(1900, 1, 1);

    private final TrainingSessionRepository trainingRepository;
    private final UserService userService;
    private final CurrentUser currentUser;

    public TrainingService(TrainingSessionRepository trainingRepository,
                           UserService userService,
                           CurrentUser currentUser) {
        this.trainingRepository = trainingRepository;
        this.userService = userService;
        this.currentUser = currentUser;
    }

    public TrainingResponse create(TrainingRequest request) {
        AppUser user = userService.getEntityById(currentUser.id());
        TrainingSession trainingSession = new TrainingSession(
                user,
                request.type(),
                request.trainedOn(),
                request.durationMinutes()
        );

        trainingSession.setLearnedNotes(request.learnedNotes());

        return new TrainingResponse(trainingRepository.save(trainingSession));
    }

    @Transactional(readOnly = true)
    public List<TrainingResponse> list(LocalDate from, LocalDate to) {
        LocalDate start = (from != null) ? from : EARLIEST_DATE;
        LocalDate end = (to != null) ? to : LocalDate.now();

        if (start.isAfter(end)) {
            throw new InvalidPeriodException("from must be before or equal to to");
        }

        return trainingRepository
                .findByUserIdAndTrainedOnBetweenOrderByTrainedOnDesc(currentUser.id(), start, end)
                .stream()
                .map(TrainingResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrainingResponse findById(UUID id) {
        TrainingSession session = getOwnedTraining(id);
        return new TrainingResponse(session);
    }

    public TrainingResponse update(UUID id, TrainingRequest request) {
        TrainingSession session = getOwnedTraining(id);
        session.setDurationMinutes(request.durationMinutes());
        session.setLearnedNotes(request.learnedNotes());
        session.setTrainedOn(request.trainedOn());
        session.setType(request.type());

        return new TrainingResponse(session);
    }

    public void delete(UUID id) {
        TrainingSession session = getOwnedTraining(id);
        trainingRepository.delete(session);
    }

    private TrainingSession getOwnedTraining(UUID id) {
        return trainingRepository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("Training Session not found"));
    }
}
