package tonelotti.dev.meujiu;

import org.springframework.boot.SpringApplication;

public class TestMeujiuApplication {

	public static void main(String[] args) {
		SpringApplication.from(MeujiuApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
