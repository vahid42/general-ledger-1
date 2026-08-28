package com.ledger.bootstrap;

import com.ledger.application.accountheading.*;
import com.ledger.domain.accountheading.AccountHeadingRepository;
import com.ledger.infrastructure.accountheading.InMemoryAccountHeadingRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class AccountHeadingConfiguration {

    @Bean
    public AccountHeadingRepository accountHeadingRepository() {
        return new InMemoryAccountHeadingRepository();
    }

    @Bean
    public CreateAccountHeadingService createAccountHeadingService(
            AccountHeadingRepository repository    
    ) {
        return new CreateAccountHeadingService(
                repository
        );
    }

    @Bean
    public DeleteAccountHeadingService deleteAccountHeadingService(
            AccountHeadingRepository repository
    ) {
        return new DeleteAccountHeadingService(repository);
    }

    @Bean
    public SearchAccountHeadingsService searchAccountHeadingsService(
            AccountHeadingRepository repository
    ) {
        return new SearchAccountHeadingsService(repository);
    }

    @Bean
    public UpdateAccountHeadingService updateAccountHeadingService(
            AccountHeadingRepository repository
    ) {
        return new UpdateAccountHeadingService(repository);
    }
}