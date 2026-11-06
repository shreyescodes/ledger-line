/**
 * Code shared by every service: money, ids, the event envelope and idempotency helpers.
 *
 * <p>Plain Java only. Nothing here may depend on Spring or any other framework, so the domain
 * code in each service can use it freely. Money arrives in Stage 2.
 */
package dev.ledgerline.common;
