package dev.ledgerline.testsupport.containers;

import org.testcontainers.utility.DockerImageName;

/** Container images used by tests. Keep them in step with infra/compose/compose.yaml. */
public final class ContainerImages {

    public static final DockerImageName POSTGRES = DockerImageName.parse("postgres:18.6-alpine");

    private ContainerImages() {}
}
