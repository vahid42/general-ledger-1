package com.ledger.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ledger")
public class GeneralLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                GeneralLedgerApplication.class,
                args
        );
    }
}