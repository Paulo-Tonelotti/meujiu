package tonelotti.dev.meujiu.academy;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "academy")
public class Academy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String city;

    protected Academy() {}

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
