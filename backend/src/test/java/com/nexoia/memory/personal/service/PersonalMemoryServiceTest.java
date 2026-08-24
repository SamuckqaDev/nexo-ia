package com.nexoia.memory.personal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.memory.personal.exception.PersonalMemoryLimitException;
import com.nexoia.memory.personal.exception.PersonalMemoryNotFoundException;
import com.nexoia.memory.personal.model.PersonalMemory;
import com.nexoia.memory.personal.repository.PersonalMemoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersonalMemoryServiceTest {

    @Mock private PersonalMemoryRepository memories;
    @Mock private AuditService audit;

    private PersonalMemoryService service;

    @BeforeEach
    void setUp() {
        service = new PersonalMemoryService(memories, audit);
    }

    @Test
    void storesNormalizedMemoryUnderTheAuthenticatedOwner() {
        UUID userId = UUID.randomUUID();
        when(memories.saveAndFlush(any(PersonalMemory.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.remember(userId, "  Prefers   concise answers  ", null, null);

        assertThat(response.content()).isEqualTo("Prefers concise answers");
        verify(memories).saveAndFlush(any(PersonalMemory.class));
    }

    @Test
    void reusesAnEquivalentOwnedMemoryInsteadOfDuplicatingIt() {
        UUID userId = UUID.randomUUID();
        PersonalMemory existing = PersonalMemory.builder()
                .id(UUID.randomUUID()).userId(userId).content("Prefers concise answers").build();
        when(memories.findFirstByUserIdAndContentIgnoreCase(userId, "prefers concise answers"))
                .thenReturn(Optional.of(existing));

        var response = service.remember(userId, "prefers concise answers", null, null);

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(memories, never()).saveAndFlush(any());
    }

    @Test
    void enforcesTheOwnerBoundaryWhenRemovingMemory() {
        UUID userId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();

        assertThatThrownBy(() -> service.remove(userId, memoryId))
                .isInstanceOf(PersonalMemoryNotFoundException.class);
        verify(memories, never()).delete(any());
    }

    @Test
    void rejectsMemoryBeyondThePersonalLimit() {
        UUID userId = UUID.randomUUID();
        when(memories.countByUserId(userId)).thenReturn((long) PersonalMemoryService.MAX_MEMORIES);

        assertThatThrownBy(() -> service.remember(userId, "Another preference", null, null))
                .isInstanceOf(PersonalMemoryLimitException.class);
    }
}
