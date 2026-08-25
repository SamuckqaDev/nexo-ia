package com.nexoia;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Starts the whole application against a disposable PostgreSQL instance.
 *
 * <p>Unit tests construct their collaborators directly, so they prove logic but never prove that the
 * container can supply a bean. This test exists because an injected dependency that does not exist
 * compiles, passes every unit test, and only fails when the context starts.
 *
 * <p>Tagged {@code docker} because Testcontainers needs a reachable Docker daemon. The container
 * image builds itself inside a builder that has no daemon, so its Maven run excludes this tag.
 */
@Tag("docker")
@Testcontainers
@SpringBootTest(classes = NexoApplication.class, properties = {
        "nexo.security.token.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "spring.mail.host=127.0.0.1",
        "spring.mail.port=1025"
})
class NexoApplicationContextTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg18-bookworm")
                    .asCompatibleSubstituteFor("postgres"));

    @Autowired
    private ApplicationContext context;
    @Autowired
    private DataSource dataSource;

    @Test
    void startsEveryBeanOfTheApplication() {
        assertThat(context.getBean("springAiChatCompletionClient")).isNotNull();
        assertThat(context.getBean("springAiModelFactory")).isNotNull();
        assertThat(context.getBean("modelRequestService")).isNotNull();
        assertThat(context.getBean("modelRequestStore")).isNotNull();
        assertThat(context.getBean("conversationContextAssembler")).isNotNull();
        assertThat(context.getBean("agentPlanToolFactory")).isNotNull();
        assertThat(context.getBean("rememberToolFactory")).isNotNull();
        assertThat(context.getBean("personalMemoryService")).isNotNull();
        assertThat(context.getBean("springAiMcpClientFactory")).isNotNull();
        assertThat(context.getBean("mcpConnectionService")).isNotNull();
        assertThat(context.getBean("auditService")).isNotNull();
    }

    @Test
    void appliesEveryMigrationToAnEmptyDatabase() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Integer applied = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        Integer activeRequestIndex = jdbc.queryForObject("""
                SELECT count(*) FROM pg_indexes
                WHERE indexname = 'ux_conversation_message_active_request'
                """, Integer.class);

        assertThat(applied).isEqualTo(33);
        assertThat(activeRequestIndex).isEqualTo(1);
    }
}
