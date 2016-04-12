package com.yahoo.javatraining.project2;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class represents a ticket. Ticket objects are produced and consumed by the storage system.
 */
public class Ticket {
    private String id;

    private String userId;

    private String holdTransId;

    private String buyTransId;

    private TicketStatusCode status = TicketStatusCode.AVAILABLE;

    private long holdTime;

    /**
     * Constructs a Ticket object with the unique ticket id.
     * This id exists in the file supplied to the TicketManager.
     *
     * @param id A ticket id.
     */
    public Ticket(@NotNull String id) {
        this.id = id;
    }

    /**
     * Returns the ticket id specified in the constructor.
     *
     * @return A ticket id.
     */
    public
    @NotNull
    String getId() {
        return id;
    }

    /**
     * Returns the ticket's current status. If the ticket status was never set, it defaults to AVAILABLE.
     *
     * @return A ticket status.
     */
    public
    @NotNull
    TicketStatusCode getStatus() {
        return status;
    }

    /**
     * Sets the ticket's status. There is no validation of the new state.
     *
     * @param status A ticket status.
     */
    public void setStatus(@NotNull TicketStatusCode status) {
        this.status = status;
    }

    /**
     * Returns the user id of the user holding or buying the ticket. The user id is null if the state is
     * AVAILABLE.
     *
     * @return Possibly-null user id.
     */
    public
    @Nullable
    String getUserId() {
        return userId;
    }

    /**
     * Sets the user id of the user holding or buying this ticket.
     *
     * @param userId A user id.
     */
    public void setUserId(@Nullable String userId) {
        this.userId = userId;
    }

    /**
     * Returns the hold transaction id of the ticket. The transaction id is null if the state is AVAILABLE.
     *
     * @return A transaction id.
     */
    public
    @Nullable
    String getHoldTransId() {
        return holdTransId;
    }

    /**
     * Sets the hold transaction id of the ticket. This transaction id is set when the ticket is held by a user.
     *
     * @param holdTransId A transaction id.
     */
    public void setHoldTransId(@Nullable String holdTransId) {
        this.holdTransId = holdTransId;
    }

    /**
     * Returns the UTC timestamp of the time that the ticket was held. This is used to cancel a hold if it's
     * held for too long. Zero is returned if the ticket is AVAILABLE.
     *
     * @return The hold timestamp.
     */
    public long getHoldTime() {
        return holdTime;
    }

    /**
     * Sets the timestamp when the ticket is held.
     *
     * @param holdTime Possibly-zero timestamp.
     */
    public void setHoldTime(long holdTime) {
        this.holdTime = holdTime;
    }

    /**
     * Returns the buy transaction id received from the buy webservice. The transaction id is null unless the state
     * is BOUGHT.
     *
     * @return A transaction id.
     */
    public @Nullable String getBuyTransId() {
        return buyTransId;
    }

    /**
     * Sets the buy transaction id received from the buy webservice.
     *
     * @param buyTransId A transaction id.
     */
    public void setBuyTransId(@Nullable String buyTransId) {
        this.buyTransId = buyTransId;
    }
}
