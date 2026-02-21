package com.example.digitalbank;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@SpringBootApplication
@EnableJpaRepositories(considerNestedRepositories = true)
@EntityScan(basePackageClasses = DigitalBankApplication.class)
public class DigitalBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalBankApplication.class, args);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://economia.awesomeapi.com.br")
                .build();
    }

    enum AccountType { CORRENTE, POUPANCA }
    enum TransactionType { DEPOSITO, SAQUE, TRANSFERENCIA }

    @Entity
    @Data
    static class Account {
        @Id
        @GeneratedValue
        private UUID id;

        @NotBlank
        private String holderName;

        @Enumerated(EnumType.STRING)
        private AccountType type;

        private BigDecimal balance = BigDecimal.ZERO;
    }

    @Entity
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class Transaction {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Enumerated(EnumType.STRING)
        private TransactionType type;

        private BigDecimal amount;
        private LocalDateTime date;
        private UUID sourceAccountId;
        private UUID destinationAccountId;
    }

    @Repository
    interface AccountRepository extends JpaRepository<Account, UUID> {}

    @Repository
    interface TransactionRepository extends JpaRepository<Transaction, Long> {
        List<Transaction> findBySourceAccountIdOrDestinationAccountId(UUID source, UUID destination);
    }

    @Service
    @RequiredArgsConstructor
    static class BankService {

        private final AccountRepository accountRepo;
        private final TransactionRepository transactionRepo;
        private final WebClient webClient;

        public Account createAccount(Account account) {
            account.setBalance(BigDecimal.ZERO);
            return accountRepo.save(account);
        }

        public List<Account> getAllAccounts() {
            return accountRepo.findAll();
        }

        public BigDecimal getBalance(UUID id) {
            return getAccount(id).getBalance();
        }

        public Account deposit(UUID id, BigDecimal amount) {
            Account acc = getAccount(id);
            acc.setBalance(acc.getBalance().add(amount));
            accountRepo.save(acc);
            saveTransaction(TransactionType.DEPOSITO, amount, id, null);
            return acc;
        }

        public Account withdraw(UUID id, BigDecimal amount) {
            Account acc = getAccount(id);
            if (acc.getBalance().compareTo(amount) < 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente");
            acc.setBalance(acc.getBalance().subtract(amount));
            accountRepo.save(acc);
            saveTransaction(TransactionType.SAQUE, amount, id, null);
            return acc;
        }

        public void transfer(UUID from, UUID to, BigDecimal amount) {
            withdraw(from, amount);
            deposit(to, amount);
            saveTransaction(TransactionType.TRANSFERENCIA, amount, from, to);
        }

        public List<Transaction> getTransactions(UUID id) {
            return transactionRepo.findBySourceAccountIdOrDestinationAccountId(id, id);
        }

        private Account getAccount(UUID id) {
            return accountRepo.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
        }

        private void saveTransaction(TransactionType type, BigDecimal amount, UUID source, UUID dest) {
            transactionRepo.save(new Transaction(null, type, amount, LocalDateTime.now(), source, dest));
        }

        public String getDollarRate() {
            return webClient.get()
                    .uri("/json/last/USD-BRL")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        }
    }

    @RestController
    @RequestMapping("/accounts")
    @RequiredArgsConstructor
        static class BankController {

        private final BankService service;

        @PostMapping
        public Account create(@RequestBody Account account) {
            return service.createAccount(account);
        }

        @GetMapping
        public List<Account> list() {
            return service.getAllAccounts();
        }

        @GetMapping("/{id}/balance")
        public BigDecimal balance(@PathVariable UUID id) {
            return service.getBalance(id);
        }

        @PostMapping("/{id}/deposit")
        public Account deposit(@PathVariable UUID id, @RequestParam BigDecimal amount) {
            return service.deposit(id, amount);
        }

        @PostMapping("/{id}/withdraw")
        public Account withdraw(@PathVariable UUID id, @RequestParam BigDecimal amount) {
            return service.withdraw(id, amount);
        }

        @PostMapping("/transfer")
        public ResponseEntity<String> transfer(@RequestParam UUID from,
                                               @RequestParam UUID to,
                                               @RequestParam BigDecimal amount) {
            service.transfer(from, to, amount);
            return ResponseEntity.ok("Transferência realizada com sucesso");
        }

        @GetMapping("/{id}/transactions")
        public List<Transaction> transactions(@PathVariable UUID id) {
            return service.getTransactions(id);
        }

        @GetMapping("/exchange/usd")
        public String dollarRate() {
            return service.getDollarRate();
        }
    }
}
