package com.yahoo.javatraining.project2;

import com.yahoo.javatraining.project2.util.Storage;
import com.yahoo.javatraining.project2.util.WebService;

/**
 * The ticket manager is used to manage the purchase of tickets.
 * This library assumes that the state of the tickets is stored in a storage system
 * and that tickets are purchased via a webservice call.
 * Furthermore, the ticket manager does not assume that the storage system is thread-safe;
 * it will carefully synchronize all calls to the storage system.
 * Also, the ticket manager will make concurrent calls to the webservice, in order to
 * minimize latency.
 */
public class TicketManager {

    /**
     * Constructs a ticket manager
     * @param storage    Non-null storage instance for storing updates to the tickets.
     * @param webservice Non-null service to use for purchases.
     */
    public TicketManager(long expireTimeMs, Storage storage, WebService webservice) throws TicketManagerException {
    }

    /**
     * Rejects further calls to this class and shutdown on-going concurrent tasks.
     * The object is no longer usable after this call.
     * @throws InterruptedException If the shutdown was interrupted.
     */
    public void shutdown() throws InterruptedException {
    }

    /**
     * Returns the number of available tickets that can be held. This method is thread-safe.
     * @return Count of available tickets.
     */
    public int availableCount() {
        return -1;
    }

    /**
     * Holds the ticket. More specifically, sets the status to be HELD, generates a hold transaction id, and sets
     * the hold time. This method is thread-safe.
     * @param ticketId Non-null ticket id.
     * @return transaction id Non-null transaction id.
     * @throws IllegalStateException Is thrown if the hold fails.
     */
    public String hold(String userId, String ticketId) throws TicketManagerException {
        return null;
    }

    /**
     * Cancels a held ticket. The ticket's state becomes AVAILABLE, the hold transaction id is cleared, and the
     * hold time is cleared. The userId and holdTransId must match the persisted values or the cancel will fail.
     * This method is thread-safe.
     * @param ticketId Non-null ticket id.
     * @return transaction id Non-null transaction id.
     * @throws IllegalStateException Is thrown if the cancel fails.
     */
    public void cancel(String userId, String ticketId, String holdTransId) throws TicketManagerException {
    }

    /**
     * Buys a held ticket. The ticket's state becomes BOUGHT and the buy transaction id is set.
     * The userId and holdTransId must match the persisted values or the buy will fail.
     * This method is thread-safe.
     * @param ticketId Non-null ticket id.
     * @return transaction id Non-null purchase transaction id.
     * @throws IllegalStateException Is thrown if the cancel fails.
     */
    public String buy(String userId, String ticketId, String holdTransId) throws TicketManagerException, InterruptedException {
        return null;
    }

    /**
     * Blocks until there are no more tickets available.
     * This method is thread-safe.
     * @throws InterruptedException If the thread is interrupted.
     */
    public void awaitSoldOut() throws InterruptedException {
    }
}
