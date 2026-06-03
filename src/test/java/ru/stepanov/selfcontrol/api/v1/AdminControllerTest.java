package ru.stepanov.selfcontrol.api.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import ru.stepanov.selfcontrol.api.contract.ErrorCode;
import ru.stepanov.selfcontrol.api.v1.support.ContractControllerTestSupport;
import ru.stepanov.selfcontrol.audit.AuditService;
import ru.stepanov.selfcontrol.identity.Email;
import ru.stepanov.selfcontrol.identity.User;
import ru.stepanov.selfcontrol.identity.UserRepository;
import ru.stepanov.selfcontrol.identity.UserRole;
import ru.stepanov.selfcontrol.identity.UserStatus;
import ru.stepanov.selfcontrol.scenario.ScenarioTemplate;
import ru.stepanov.selfcontrol.scenario.ScenarioTemplateRepository;
import ru.stepanov.selfcontrol.security.AuthenticationFacade;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserRepository users;
    @Mock
    private ScenarioTemplateRepository templates;
    @Mock
    private AuditService audit;
    @Mock
    private AuthenticationFacade auth;

    @InjectMocks
    private AdminController controller;

    private MockMvc mockMvc;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        mockMvc = ContractControllerTestSupport.mockMvc(controller);
        adminId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void listUsersReturnsPagedContractFields() throws Exception {
        User user = sampleUser();
        when(users.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(user.getUserId().toString()))
                .andExpect(jsonPath("$.content[0].email").value("admin@selfcontrol.local"))
                .andExpect(jsonPath("$.content[0].firstName").value("Admin"))
                .andExpect(jsonPath("$.content[0].status").value("Active"))
                .andExpect(jsonPath("$.meta.page").value(0));
    }

    @Test
    void updateUserStatusReturns200() throws Exception {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User user = sampleUser();
        user.setUserId(userId);
        when(auth.userId()).thenReturn(adminId);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"Blocked"}
                                """))
                .andExpect(status().isOk());

        verify(audit).record(eq(adminId), eq(userId), eq("USER_BLOCKED"), eq("USER"), eq(userId), any());
    }

    @Test
    void updateUserStatusNotFoundReturns404ErrorResponse() throws Exception {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(users.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"Blocked"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value(ErrorCode.NOT_FOUND.name()));
    }

    @Test
    void createScenarioTemplateReturns201WithContractFields() throws Exception {
        when(auth.userId()).thenReturn(adminId);
        when(templates.save(any(ScenarioTemplate.class))).thenAnswer(invocation -> {
            ScenarioTemplate template = invocation.getArgument(0);
            template.setScenarioId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
            return template;
        });

        mockMvc.perform(post("/api/v1/admin/scenarios/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioTypeCode":"UNDESIRABLE_PURCHASE","name":"Нежелательные покупки","description":"Описание"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateId").value("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
                .andExpect(jsonPath("$.scenarioTypeCode").value("UNDESIRABLE_PURCHASE"))
                .andExpect(jsonPath("$.isPublished").value(true));
    }

    private User sampleUser() {
        User user = new User();
        user.setUserId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        user.setEmail(new Email("admin@selfcontrol.local"));
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setRole(UserRole.User);
        user.setStatus(UserStatus.Active);
        user.setCreatedAt(Instant.parse("2026-06-02T00:00:00.000Z"));
        return user;
    }
}
