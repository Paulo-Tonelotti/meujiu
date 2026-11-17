package tonelotti.dev.meujiu.training;

import jakarta.persistence.*;
import tonelotti.dev.meujiu.training.enums.TrainingType;
import tonelotti.dev.meujiu.user.AppUser;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "training_session")
public class TrainingSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",  nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    private TrainingType type;

    private LocalDate trainedOn;
    private int durationMinutes;
    private String learnedNotes;
    private Instant createdAt;

    protected TrainingSession() {}

    public TrainingSession(AppUser user, TrainingType type, LocalDate trainedOn, int durationMinutes) {
        this.user = user;
        this.type = type;
        this.trainedOn = trainedOn;
        this.durationMinutes = durationMinutes;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public TrainingType getType() {
        return type;
    }

    public LocalDate getTrainedOn() {
        return trainedOn;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getLearnedNotes() {
        return learnedNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setType(TrainingType type) {
        this.type = type;
    }

    public void setTrainedOn(LocalDate trainedOn) {
        this.trainedOn = trainedOn;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setLearnedNotes(String learnedNotes) {
        this.learnedNotes = learnedNotes;
    }
}
