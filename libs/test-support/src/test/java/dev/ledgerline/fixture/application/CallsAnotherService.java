package dev.ledgerline.fixture.application;

import dev.ledgerline.otherservice.OtherServiceType;

/** Breaks the service boundary rule. */
public final class CallsAnotherService {

    public OtherServiceType other() {
        return new OtherServiceType();
    }
}
