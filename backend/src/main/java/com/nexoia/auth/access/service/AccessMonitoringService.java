package com.nexoia.auth.access.service;

import com.nexoia.auth.access.dto.AccessEventResponse;
import com.nexoia.auth.access.model.AccessEvent;
import com.nexoia.auth.access.repository.AccessEventRepository;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessMonitoringService {

    private final AccessEventRepository accessEventRepository;

    @Transactional(readOnly = true)
    public List<AccessEventResponse> recentEvents(NexoUserPrincipal principal) {
        return accessEventRepository.findTop100ByUserIdOrderByOccurredAtDesc(principal.userId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AccessEventResponse toResponse(AccessEvent event) {
        return new AccessEventResponse(event.getId(), event.getSessionId(), event.getEventType(),
                event.isSuccess(), event.getIpAddress(), event.getUserAgent(), event.getOccurredAt());
    }
}
