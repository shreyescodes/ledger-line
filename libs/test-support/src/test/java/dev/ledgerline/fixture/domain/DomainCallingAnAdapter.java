package dev.ledgerline.fixture.domain;

import dev.ledgerline.fixture.adapter.FixtureAdapter;

/** Breaks the layer rule: the domain reaches out to an adapter. */
public final class DomainCallingAnAdapter {

    public FixtureAdapter adapter() {
        return new FixtureAdapter();
    }
}
