package com.ledger.presentation.accountheading;

import com.ledger.application.accountheading.*;


import com.ledger.presentation.accountheading.create.CreateAccountHeadingInput;
import com.ledger.presentation.accountheading.create.CreateAccountHeadingMapper;
import com.ledger.presentation.accountheading.create.CreateAccountHeadingOutput;

import com.ledger.presentation.accountheading.delete.DeleteAccountHeadingInput;
import com.ledger.presentation.accountheading.delete.DeleteAccountHeadingMapper;
import com.ledger.presentation.accountheading.delete.DeleteAccountHeadingOutput;

import com.ledger.presentation.accountheading.search.SearchAccountHeadingsInput;
import com.ledger.presentation.accountheading.search.SearchAccountHeadingsMapper;
import com.ledger.presentation.accountheading.search.SearchAccountHeadingsOutput;

import com.ledger.presentation.accountheading.update.UpdateAccountHeadingInput;
import com.ledger.presentation.accountheading.update.UpdateAccountHeadingMapper;
import com.ledger.presentation.accountheading.update.UpdateAccountHeadingOutput;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/account-headings")
public class AccountHeadingController {

    private final CreateAccountHeadingService createService;
    private final UpdateAccountHeadingService updateService;
    private final DeleteAccountHeadingService deleteService;
    private final SearchAccountHeadingsService searchService;

    private final CreateAccountHeadingMapper createMapper;
    private final UpdateAccountHeadingMapper updateMapper;
    private final DeleteAccountHeadingMapper deleteMapper;
    private final SearchAccountHeadingsMapper searchMapper;

    public AccountHeadingController(
            CreateAccountHeadingService createService,
            UpdateAccountHeadingService updateService,
            DeleteAccountHeadingService deleteService,
            SearchAccountHeadingsService searchService,
            CreateAccountHeadingMapper createMapper,
            UpdateAccountHeadingMapper updateMapper,
            DeleteAccountHeadingMapper deleteMapper,
            SearchAccountHeadingsMapper searchMapper
    ) {
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.searchService = searchService;

        this.createMapper = createMapper;
        this.updateMapper = updateMapper;
        this.deleteMapper = deleteMapper;
        this.searchMapper = searchMapper;
    }

    // =========================================================
    // Create
    // =========================================================

    @PostMapping
    public CreateAccountHeadingOutput create(
            @RequestBody CreateAccountHeadingInput input
    ) {

        CreateAccountHeadingRequest request =
                createMapper.toRequest(input);

        CreateAccountHeadingResponse response =
                createService.create(request);

        return createMapper.toOutput(response);
    }

    // =========================================================
    // Update
    // =========================================================

    @PutMapping("/{id}")
    public UpdateAccountHeadingOutput update(
            @PathVariable String id,
            @RequestBody UpdateAccountHeadingInput input
    ) {

        UpdateAccountHeadingInput mappedInput =
                new UpdateAccountHeadingInput(
                        id,
                        input.name()
                );

        UpdateAccountHeadingRequest request =
                updateMapper.toRequest(mappedInput);

        UpdateAccountHeadingResponse response =
                updateService.execute(request);

        return updateMapper.toOutput(response);
    }

    // =========================================================
    // Delete
    // =========================================================

    @DeleteMapping("/{id}")
    public DeleteAccountHeadingOutput delete(
            @PathVariable String id
    ) {

        DeleteAccountHeadingInput input =
                new DeleteAccountHeadingInput(id);

        DeleteAccountHeadingRequest request =
                deleteMapper.toRequest(input);

        DeleteAccountHeadingResponse response =
                deleteService.execute(request);

        return deleteMapper.toOutput(response);
    }

    // =========================================================
    // Search
    // =========================================================

     @GetMapping
        public List<SearchAccountHeadingsOutput> search(
                @RequestParam(required = false) String code,
                @RequestParam(required = false) String name,
                @RequestParam(required = false) String parentId,
                @RequestParam(required = false) Integer level,
                @RequestParam(required = false) Boolean leaf,
                @RequestParam(required = false) String nature
        ) {

        SearchAccountHeadingsInput input =
                new SearchAccountHeadingsInput(
                        code,
                        name,
                        parentId,
                        level,
                        leaf,
                        nature
                );

        SearchAccountHeadingsRequest request =
                searchMapper.toRequest(input);

        SearchAccountHeadingsResponse response =
                searchService.execute(request);

        return searchMapper.toOutput(
                response.items()
        );
        }
}