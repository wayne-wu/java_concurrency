package com.yahoo.javatraining.project2.util;

import java.util.UUID;

/**
 * This is a client library that accesses a webservice for purchasing tickets.
 */
public class WebService {
    /**
     * This method is called to purchase a ticket. It will block until the purchase is confirmed.
     * If the ticket was already purchased by the user, the previous buy transaction id is returned.
     * This method is thread-safe; it can be called by up to 5 concurrent threads.
     * @param ticketId Non-null ticket id.
     * @param userId Non-null user id.
     * @return Non-null buy transaction id.
     */
    public String buy(String ticketId, String userId) {
        try {
            Thread.sleep((int)(Math.random() * 100)); // artificial delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // TODO: If the ticket was already purchased, the previous buy transaction id should be returned.
        // This mock implementation does not properly implement that behavior.
        return UUID.randomUUID().toString();
    }
}
