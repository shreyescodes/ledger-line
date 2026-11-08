package dev.ledgerline.common.id;

import java.util.UUID;

/** Source of new ids. Production uses time-ordered UUIDv7 (ADR-0011); tests may plug in their own. */
@FunctionalInterface
public interface IdGenerator {

    UUID next();
}
