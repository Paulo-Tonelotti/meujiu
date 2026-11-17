package tonelotti.dev.meujiu.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tonelotti.dev.meujiu.TestcontainersConfiguration;
import tonelotti.dev.meujiu.user.AppUser;
import tonelotti.dev.meujiu.user.repository.UserRepository;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.*;


@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired 
    UserRepository userRepository;

    @Autowired 
    PasswordEncoder passwordEncoder;

    private static final String ACADEMY_ID = "11111111-1111-1111-1111-111111111111";

    @Test
    void register_dataValid_return201_withoutExposedPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Paulo", "paulo@teste.com.br", "12345678")))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Paulo"))
                .andExpect(jsonPath("$.email").value("paulo@teste.com.br"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void register_shortPassword_return400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Paulo", "paulo@teste.com.br", "123456")))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')]").exists());
    }

    @Test
    void register_upperCaseEmail_return201_withEmailNormalized() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Paulo", "Paulo@teste.com.br", "12345678")))



                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("paulo@teste.com.br"));
    }

    @Test
    void register_emailAlreadyExists_return409() throws Exception {
        String json = registerJson("Paulo", "paulo@teste.com.br", "12345678");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))

                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This email already in use"))
                .andExpect(jsonPath("$.error").value("Data conflict"));
    }

    @Test
    void register_unknownAcademy_return404() throws Exception {
        String json = """
            {
                "name": "Paulo",
                "email": "paulo@teste.com.br",
                "password": "12345678",
                "academyId": "11111111-2222-1111-1111-111111111111"
            }
        """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Academy Not Found"))
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    void register_missingAcademy_return400() throws Exception {
        String json = """
            {
                "name": "Paulo",
                "email": "paulo@teste.com.br",
                "password": "12345678"
            }
        """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'academyId' && @.message == 'Academy is required')]").exists());
    }

    @Test
    void register_invalidEmail_return400() throws Exception {
        String json = """
            {
                "name": "Paulo",
                "email": "pauloteste.com.br",
                "password": "12345678",
                "academyId": "11111111-1111-1111-1111-111111111111"
            }
        """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'email' && @.message == 'Invalid email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'email' && @.message == 'Email must be valid and contain a domain')]").exists());
    }

    @Test 
    void register_blankName_return400() throws Exception {
        String json = """
            {
                "name": "",
                "email": "pauloteste.com.br",
                "password": "12345678",
                "academyId": "11111111-1111-1111-1111-111111111111"
            }
        """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'name' && @.message == 'Name is required')]").exists());
    }

    @Test 
    void register_validData_savePasswordHashed() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Paulo", "paulo@teste.com.br", "12345678")))
                .andExpect(status().isCreated());
        
        AppUser saved = userRepository.findByEmail("paulo@teste.com.br").orElseThrow();

        assertThat(saved.getPasswordHash()).isNotEqualTo("12345678");
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("12345678", saved.getPasswordHash())).isTrue();
        
    }

    @Test
    void register_blankEmail_return400() throws Exception {
        String json = """
            {
                "name": "Paulo Tonelotti",
                "email": "",
                "password": "12345678",
                "academyId": "11111111-1111-1111-1111-111111111111"
            }
        """;

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content((json)))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'email' && @.message == 'Email is required')]").exists());
    }

    @Test
    void register_longPassword_return400() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Paulo",
                        "paulo@teste.com.br",
                        "k9#mP2$vX8zLq4!wR7bN5*jF1hD3gC6sT9yX2zLq4!wR7bN5*jF1hD3gC6sT9yX2zLq4!wR7bN5*")))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'password' && @.message == 'Password must be between 8 and 72 characters')]").exists());
    }

    @Test
    void register_longName_return400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Alexander Bartholomew Montgomery-Fitzwilliam Cornelius Harrington-Smythe the Third, Junior Esquire of the Northern Highlands",
                                "paulo@teste.com.br",
                                "12345678")))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'name' && @.message == 'Name must be at most 120 characters')]").exists());
    }

    private String registerJson(String name, String email, String password) {
        return """
        {"name": "%s", "email": "%s", "password": "%s", "academyId": "%s"}
        """.formatted(name, email, password, ACADEMY_ID);
    }
}