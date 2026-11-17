package tonelotti.dev.meujiu.user;

import jakarta.persistence.*;
import tonelotti.dev.meujiu.academy.Academy;
import tonelotti.dev.meujiu.user.enums.Role;

import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String name;
    private String email;
    private String passwordHash;

    protected AppUser() {}

    public AppUser(Academy academy, String name, String email, String passwordHash) {
        this.academy = academy;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = Role.STUDENT;
    }

    public UUID getId() {
        return id;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public Academy getAcademy() {
        return academy;
    }

    public Role getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
