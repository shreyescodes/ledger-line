package dev.ledgerline.reconciliation;

import dev.ledgerline.testsupport.actuator.ActuatorContractTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReconciliationActuatorTest extends ActuatorContractTest {}
