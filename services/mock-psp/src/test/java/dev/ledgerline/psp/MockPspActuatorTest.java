package dev.ledgerline.psp;

import dev.ledgerline.testsupport.actuator.ActuatorContractTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MockPspActuatorTest extends ActuatorContractTest {}
