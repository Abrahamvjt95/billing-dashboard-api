package com.abrahamjaimes.billing.service;

import com.abrahamjaimes.billing.dto.request.ClientRequest;
import com.abrahamjaimes.billing.dto.response.ClientResponse;
import com.abrahamjaimes.billing.dto.response.PageResponse;
import com.abrahamjaimes.billing.entity.Client;
import com.abrahamjaimes.billing.entity.Role;
import com.abrahamjaimes.billing.entity.User;
import com.abrahamjaimes.billing.exception.NotFoundException;
import com.abrahamjaimes.billing.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock ClientRepository clientRepository;
    @InjectMocks ClientService clientService;

    private User owner;
    private Client sampleClient;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).email("owner@test.com")
                .role(Role.USER).enabled(true).build();

        sampleClient = Client.builder()
                .id(10L).owner(owner).name("Acme Corp")
                .email("acme@test.com").phone("555-1234").build();
    }

    @Test
    void findAll_returnsPaginatedClients() {
        var page = new PageImpl<>(List.of(sampleClient));
        when(clientRepository.findAllByOwnerId(eq(1L), any(Pageable.class))).thenReturn(page);

        PageResponse<ClientResponse> result = clientService.findAll(owner, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().name()).isEqualTo("Acme Corp");
    }

    @Test
    void findById_returnsClient_whenOwnerMatches() {
        when(clientRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sampleClient));

        ClientResponse response = clientService.findById(owner, 10L);

        assertThat(response.name()).isEqualTo("Acme Corp");
    }

    @Test
    void findById_throwsNotFound_whenClientBelongsToOtherUser() {
        when(clientRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.findById(owner, 10L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_savesAndReturnsClient() {
        var request = new ClientRequest("New Client", "new@test.com", "555", "Addr", "TAX1", "notes");
        when(clientRepository.save(any(Client.class))).thenReturn(sampleClient);

        ClientResponse response = clientService.create(owner, request);

        assertThat(response).isNotNull();
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void update_modifiesAndReturnsClient() {
        var request = new ClientRequest("Updated", "upd@test.com", null, null, null, null);
        when(clientRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sampleClient));
        when(clientRepository.save(any())).thenReturn(sampleClient);

        clientService.update(owner, 10L, request);

        assertThat(sampleClient.getName()).isEqualTo("Updated");
        verify(clientRepository).save(sampleClient);
    }

    @Test
    void delete_removesClient() {
        when(clientRepository.findByIdAndOwnerId(10L, 1L)).thenReturn(Optional.of(sampleClient));

        clientService.delete(owner, 10L);

        verify(clientRepository).delete(sampleClient);
    }
}
