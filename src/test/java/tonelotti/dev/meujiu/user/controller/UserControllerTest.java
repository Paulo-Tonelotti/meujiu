package tonelotti.dev.meujiu.user.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tonelotti.dev.meujiu.TestcontainersConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    private static final String ACADEMY_ID = "11111111-1111-1111-1111-111111111111";

    // ======================================GET TESTS================================================

    @Test
    void me_returnDataUserLogged_return200() throws Exception {
        String id = registerAndGetId("paulo@teste.com.br");

        mockMvc.perform(get("/me").header("X-User-Id", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paulo"))
                .andExpect(jsonPath("$.email").value("paulo@teste.com.br"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.role").value("STUDENT"));

    }

    @Test
    void me_withoutHeader_return401() throws Exception {
        mockMvc.perform(get("/me"))

                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing X-User-Id header"));
    }

    @Test
    void me_malformedUserId_return401() throws Exception {
        String id = "not-a-uuid";

        mockMvc.perform(get("/me").header("X-User-Id", id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid X-User-Id header"));
    }

    @Test
    void me_unknownUser_return404() throws Exception {
        String id = "123e4567-e89b-12d3-a456-426614174000";

        mockMvc.perform(get("/me").header("X-User-Id", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ==============================================================================================
    // ======================================PUT TESTS================================================

    @Test
    void update_validData_return200() throws Exception {
        String idUser = registerAndGetId("paulo@teste.com.br");

        mockMvc.perform(put("/me")
                .header("X-User-Id", idUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Paulo Tonelotti",
                            "email": "ti@example.com.br"
                        }
                        """))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paulo Tonelotti"))
                .andExpect(jsonPath("$.email").value("ti@example.com.br"));
    }

    @Test
    void update_upperCaseEmail_return200_withEmailNormalized() throws Exception {
        String idUser = registerAndGetId("paulo@teste.com.br");

        mockMvc.perform(put("/me")
                .header("X-User-Id", idUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Paulo Tonelotti",
                            "email": "TI@example.com.br"
                        }
                        """))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paulo Tonelotti"))
                .andExpect(jsonPath("$.email").value("ti@example.com.br"));
    }

    @Test
    void update_sameEmail_return200() throws Exception {
        String idUser = registerAndGetId("paulo@teste.com.br");

        mockMvc.perform(put("/me")
                .header("X-User-Id", idUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Paulo Tonelotti",
                            "email": "paulo@teste.com.br"
                        }
                        """))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paulo Tonelotti"))
                .andExpect(jsonPath("$.email").value("paulo@teste.com.br"));
    }

    @Test
    void update_emailOfOtherUser_return409() throws Exception {
        String idUserOne = registerAndGetId("paulo@teste.com.br");
        registerAndGetId("ti@teste.com.br");

        mockMvc.perform(put("/me")
                .header("X-User-Id", idUserOne)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Paulo Tonelotti",
                            "email": "ti@teste.com.br"
                        }
                        """))

                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This email already in use"));
    }

    @Test
    void update_dataInvalid_return400() throws Exception {
        String idUser = registerAndGetId("paulo@teste.com.br");

        mockMvc.perform(put("/me")
                        .header("X-User-Id", idUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "name": "",
                            "email": "paulo@teste.com.br"
                        }
                        """))

                .andExpect(status().isBadRequest());
    }

    @Test
    void update_withoutHeader_return401() throws Exception {
        mockMvc.perform(put("/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Paulo Tonelotti",
                            "email": "paulo@teste.com.br"
                        }
                        """))

                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_invalidEmail_return400() throws Exception {
        String idUser = registerAndGetId("ti@teste.com.br");

        mockMvc.perform(put("/me")
                .header("X-User-Id", idUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Paulo Tonelotti",
                            "email": "Titeste.com.br"
                        }
                        """))

                .andExpect(status().isBadRequest());
    }

    @Test
    void update_validData_checkReturn_return200() throws Exception {
        String idUser = registerAndGetId("paulo@teste.com.br");

        mockMvc.perform(put("/me")
                .header("X-User-Id", idUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "Paulo Tonelotti",
                            "email": "ti@teste.com.br"
                        }
                        """));

        mockMvc.perform(get("/me")
                        .header("X-User-Id", idUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paulo Tonelotti"))
                .andExpect(jsonPath("$.email").value("ti@teste.com.br"));
    }

    private String registerJson(String name, String email, String password) {
        return """
        {"name": "%s", "email": "%s", "password": "%s", "academyId": "%s"}
        """.formatted(name, email, password, ACADEMY_ID);
    }

    private String registerAndGetId(String email) throws Exception {
        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Paulo", email, "12345678")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}