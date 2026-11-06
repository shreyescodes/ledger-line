package dev.ledgerline.fixture.application;

import dev.ledgerline.common.fixture.SharedType;

/** Allowed: every service may use libs/common. */
public final class UsesTheCommonLib {

    public SharedType shared() {
        return new SharedType();
    }
}
