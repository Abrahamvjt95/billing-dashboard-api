package com.abrahamjaimes.billing.controller;

import com.abrahamjaimes.billing.dto.request.ClientRequest;
import com.abrahamjaimes.billing.dto.response.ClientResponse;
import com.abrahamjaimes.billing.dto.response.PageResponse;
import com.abrahamjaimes.billing.entity.User;
import com.abrahamjaimes.billing.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients")
@Tag(name = "Clients", description = "Manage your client directory")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @Operation(summary = "List clients (paginated)")
    public PageResponse<ClientResponse> list(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return clientService.findAll(user, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID")
    public ClientResponse get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return clientService.findById(user, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new client")
    public ClientResponse create(@AuthenticationPrincipal User user, @Valid @RequestBody ClientRequest request) {
        return clientService.create(user, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing client")
    public ClientResponse update(@AuthenticationPrincipal User user, @PathVariable Long id,
                                 @Valid @RequestBody ClientRequest request) {
        return clientService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a client")
    public void delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        clientService.delete(user, id);
    }
}
