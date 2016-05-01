package com.yahoo.javatraining.project2;

import com.yahoo.javatraining.project2.util.Storage;
import com.yahoo.javatraining.project2.util.WebService;

import javax.validation.constraints.NotNull;
import java.util.*;

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
     *
     * @param storage    A storage instance for storing updates to the tickets.
     * @param webservice A service instance to use for purchases.
     */
    public TicketManager(long expireTimeMs, @NotNull Storage storage, @NotNull WebService webservice)
            throws TicketManagerException {
    }

    /**
     * Rejects further calls to this class and shutdowns on-going concurrent tasks.
     * The object is no longer usable after this call.
     *
     * @throws InterruptedException If the shutdown was interrupted.
     */
    public void shutdown() throws InterruptedException {
    }

    public List<Ticket> tickets() {
        return null;
    }

    /**
     * Returns the number of available tickets that are in the AVAILABLE and HELD states.
     * If greater than 0, it means that the tickets have not been sold out yet.
     * This method is thread-safe.
     *
     * @return Count of available tickets.
     */
    public int availableCount() {
        return -1;
    }

    /**
     * Holds the ticket. More specifically, sets the status to be HELD, generates a hold transaction id, and sets
     * the hold time. This method is thread-safe.
     * <p>
     * The hold trans id is returned if the ticket is already being held.
     *
     * @param userId   A user id.
     * @param ticketId A ticket id.
     * @return transaction id A transaction id.
     * @throws IllegalStateException Is thrown if the hold fails.
     */
    public
    @NotNull
    String hold(@NotNull String userId, @NotNull String ticketId) throws TicketManagerException {
        return null;
    }

    /**
     * Cancels a held ticket. The ticket's state becomes AVAILABLE, the hold transaction id is cleared, and the
     * hold time is cleared. The userId and holdTransId must match the persisted values or the cancel will fail.
     * This method is thread-safe.
     * <p>
     * Returns false if the ticket has already been cancelled.
     *
     * @param userId      A user id.
     * @param ticketId    A ticket id.
     * @param holdTransId A hold transaction id.
     * @return true If the cancel succeeded.
     * @throws IllegalStateException Is thrown if the cancel fails.
     */
    public boolean cancel(@NotNull String userId, @NotNull String ticketId, @NotNull String holdTransId) throws TicketManagerException {
        return false;
    }

    /**
     * Buys a held ticket. The ticket's state becomes BOUGHT and the buy transaction id is set.
     * The userId and holdTransId must match the persisted values or the buy will fail.
     * This method is thread-safe.
     *
     * @param userId      A user id.
     * @param ticketId    A ticket id.
     * @param holdTransId A hold transaction id.
     * @return The buy transaction id.
     * @throws IllegalStateException Is thrown if the buy fails.
     */
    public
    @NotNull
    String buy(@NotNull String userId, @NotNull String ticketId, @NotNull String holdTransId)
            throws TicketManagerException, InterruptedException {
        return null;
    }

    /**
     * Blocks until all tickets are in the BOUGHT state.
     * The caller should not shutdown this instance until this method returns.
     * This method is thread-safe.
     *
     * @throws InterruptedException If the thread is interrupted.
     */
    public void awaitAllBought() throws InterruptedException {
    }
}
