package com.yahoo.javatraining.project2;

import com.yahoo.javatraining.project2.util.Storage;
import com.yahoo.javatraining.project2.util.WebService;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 */
public class ConcurrentUserTester {
    private static final int NUM_USERS = 50;
    private static final int NUM_TICKETS = 1000;

    public static void main(String[] args) throws Exception {
        File file = new File("/tmp/tickets");

        // Generate file of tickets
        try (FileWriter wr = new FileWriter(file)) {
            for (int i = 1; i <= NUM_TICKETS; i++) {
                wr.write(i + "\n");
            }
        }

        // Start up ticket manager
        TicketManager tmgr = new TicketManager(10, new Storage(file), new WebService());

        // Start up many concurrent users
        List<Ticket> tickets = new Storage(file).getTickets();
        List<Future<?>> users = new ArrayList<>();
        ExecutorService userService = Executors.newFixedThreadPool(NUM_USERS);
        AtomicBoolean stop = new AtomicBoolean();
        for (int i = 0; i < NUM_USERS; i++) {
            users.add(userService.submit(new User("user-" + i, tmgr, tickets, stop)));
        }

        // Wait until there are no more tickets
        tmgr.awaitSoldOut();

        // Now stop all the users
        stop.set(true);
        for (Future<?> u : users) {
           u.get();
        }
        tmgr.shutdown();

        // Confirm that all tickets have been purchased
        tickets = new Storage(file).getTickets();
        for (Ticket t : tickets) {
            if (t.getStatus() != TicketStatusCode.BOUGHT) {
                throw new IllegalStateException(String.format("The ticket %s was not successfully purchased"));
            }
        }
        System.exit(0);
    }
}

class User implements Runnable {
    private final String userId;
    private final TicketManager tmgr;
    private final List<Ticket> tickets;
    private final AtomicBoolean stop;

    User(String userId, TicketManager tmgr, List<Ticket> tickets, AtomicBoolean stop) {
        this.userId = userId;
        this.tmgr = tmgr;
        this.tickets = tickets;
        this.stop = stop;
    }

    public void run() {
        while (!stop.get()) {
            Ticket ticket = tickets.get(rand(tickets.size()));
            try {
                switch (rand(2)) {
                    case 0:
                        // Hold
                        String txId = tmgr.hold(userId, ticket.getId());
                        Thread.sleep(rand(50)); // possibly expire
                        tmgr.cancel(userId, ticket.getId(), txId);
                        System.out.printf("%s cancelled ticket %s\n", userId, ticket.getId());
                        break;
                    case 1:
                        // Buy
                        txId = tmgr.hold(userId, ticket.getId());
                        Thread.sleep(rand(50)); // possibly expire
                        tmgr.buy(userId, ticket.getId(), txId);
                        System.out.printf("%s completed purchase of ticket %s. %s tickets left\n",
                                userId, ticket.getId(), tmgr.availableCount());
                        break;
                    case 2:
                        tmgr.hold(userId, ticket.getId());
                        break;
                }
            } catch (TicketManagerException e) {
                System.out.println(e.getMessage());
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private int rand(int n) {
        return (int) (Math.random() * n);
    }
}