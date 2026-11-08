package dev.ledgerline.ledger.adapter.in.web;

import dev.ledgerline.common.money.CurrencyMismatchException;
import dev.ledgerline.ledger.application.PostingContentionException;
import dev.ledgerline.ledger.domain.AccountCurrencyMismatchException;
import dev.ledgerline.ledger.domain.InsufficientFundsException;
import dev.ledgerline.ledger.domain.InvalidTransactionException;
import dev.ledgerline.ledger.domain.UnknownAccountException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Turns the ledger's refusals into RFC 9457 problem responses with stable type URIs. */
@RestControllerAdvice
class LedgerProblemHandler {

    @ExceptionHandler(UnknownAccountException.class)
    ProblemDetail unknownAccount(UnknownAccountException e) {
        return problem(HttpStatus.NOT_FOUND, "unknown-account", "Unknown account", e);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail insufficientFunds(InsufficientFundsException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "insufficient-funds", "Insufficient funds", e);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    ProblemDetail invalidTransaction(InvalidTransactionException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-transaction", "Invalid transaction", e);
    }

    @ExceptionHandler({AccountCurrencyMismatchException.class, CurrencyMismatchException.class})
    ProblemDetail currencyMismatch(RuntimeException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "currency-mismatch", "Currency mismatch", e);
    }

    @ExceptionHandler(PostingContentionException.class)
    ProblemDetail contention(PostingContentionException e) {
        return problem(HttpStatus.CONFLICT, "posting-contention", "Too much contention, retry later", e);
    }

    /** Unknown ISO 4217 codes and similar bad values that passed the request's shape checks. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badValue(IllegalArgumentException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-value", "Invalid value", e);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, RuntimeException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setType(URI.create("urn:ledgerline:problem:" + type));
        problem.setTitle(title);
        return problem;
    }
}
