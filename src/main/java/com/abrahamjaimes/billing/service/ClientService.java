package com.abrahamjaimes.billing.service;

import com.abrahamjaimes.billing.dto.request.ClientRequest;
import com.abrahamjaimes.billing.dto.response.ClientResponse;
import com.abrahamjaimes.billing.dto.response.PageResponse;
import com.abrahamjaimes.billing.entity.Client;
import com.abrahamjaimes.billing.entity.User;
import com.abrahamjaimes.billing.exception.NotFoundException;
import com.abrahamjaimes.billing.repository.ClientRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> findAll(User owner, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return PageResponse.from(clientRepository.findAllByOwnerId(owner.getId(), pageable), ClientResponse::from);
    }

    @Transactional(readOnly = true)
    public ClientResponse findById(User owner, Long id) {
        return ClientResponse.from(getClientOrThrow(owner, id));
    }

    public ClientResponse create(User owner, ClientRequest request) {
        Client client = Client.builder()
                .owner(owner)
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .taxId(request.taxId())
                .notes(request.notes())
                .build();
        return ClientResponse.from(clientRepository.save(client));
    }

    public ClientResponse update(User owner, Long id, ClientRequest request) {
        Client client = getClientOrThrow(owner, id);
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setAddress(request.address());
        client.setTaxId(request.taxId());
        client.setNotes(request.notes());
        return ClientResponse.from(clientRepository.save(client));
    }

    public void delete(User owner, Long id) {
        Client client = getClientOrThrow(owner, id);
        clientRepository.delete(client);
    }

    private Client getClientOrThrow(User owner, Long id) {
        return clientRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new NotFoundException("Client not found: " + id));
    }
}
