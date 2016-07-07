package com.yahoo.javatraining.project2;

import com.yahoo.javatraining.project2.util.Storage;
import com.yahoo.javatraining.project2.util.WebService;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private Storage storage;
    private WebService webservice;
    private int availableTickets; //available tickets (HELD + AVAILABLE)
    private int unBoughtTickets; //Unbought Tickets (!BOUGHT)
    private ExecutorService executor; //for executing webservice requests
    private ScheduledExecutorService timer; //for timing and resetting expired held tickets
    private ExecutorService finisher; //for finishing the "buying" tickets
    private BlockingQueue<Ticket> heldTickets;
    private HashMap<String, Ticket> tickets;
    private Lock global = new ReentrantLock();
    private Lock storage_lock = new ReentrantLock();
    private Lock count = new ReentrantLock();
    private Condition condition = count.newCondition();

    /**
     * Constructs a ticket manager
     *
     * @param storage    A storage instance for storing updates to the tickets.
     * @param webservice A service instance to use for purchases.
     */
    public TicketManager(long expireTimeMs, @NotNull Storage storage, @NotNull WebService webservice)
            throws TicketManagerException {
        this.storage = storage;
        this.webservice = webservice;
        this.executor = Executors.newFixedThreadPool(5);
        this.timer = Executors.newScheduledThreadPool(1);
        this.finisher = Executors.newCachedThreadPool();
        this.heldTickets = new LinkedBlockingQueue<>();
        this.tickets = new HashMap<>();

        Runnable resetTask = () -> {
            try{
                Ticket ticket = heldTickets.peek();
                if(ticket.getStatus() != TicketStatusCode.HELD){
                    heldTickets.take(); //no longer held
                }
                if((System.currentTimeMillis()-ticket.getBuyingTime())>expireTimeMs){
                    System.out.println("Held Ticket is expired");
                    cancel(heldTickets.take());
                }
            }catch(TicketManagerException|InterruptedException e){
                throw new IllegalStateException();
            }
        };

        long period = (expireTimeMs<10000) ? expireTimeMs/10 : 1000;
        timer.scheduleAtFixedRate(resetTask, expireTimeMs, period, TimeUnit.MILLISECONDS);

        availableTickets=0;
        unBoughtTickets=0;
        for(Ticket tik : storage.getTickets()){
            tickets.put(tik.getId(), tik);
            if(tik.getStatus()==TicketStatusCode.HELD || tik.getStatus()==TicketStatusCode.AVAILABLE){
                availableTickets++;
            }
            if(tik.getStatus()!=TicketStatusCode.BOUGHT){
                unBoughtTickets++;
            }
            if(tik.getStatus()==TicketStatusCode.BUYING){
                finisher.submit(()->{
                    try{
                        buy(tik);
                        //System.out.printf("Completed purchase on ticket %s\n", tik.getId());
                    }catch(TicketManagerException|InterruptedException e){
                        throw new IllegalStateException();
                    }
                });
            }
        }
    }

    /**
     * Rejects further calls to this class and shutdowns on-going concurrent tasks.
     * The object is no longer usable after this call.
     *
     * @throws InterruptedException If the shutdown was interrupted.
     */
    public void shutdown() throws InterruptedException {
        //Shut down all tasks
        executor.shutdown();
        timer.shutdown();
        finisher.shutdown();
    }

    public List<Ticket> tickets() {
        return new ArrayList<>(tickets.values());
    }

    /**
     * Returns the number of available tickets that are in the AVAILABLE and HELD states.
     * If greater than 0, it means that the tickets have not been sold out yet.
     * This method is thread-safe.
     *
     * @return Count of available tickets.
     */
    public int availableCount() {
        return availableTickets;
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
        Ticket ticket = tickets.get(ticketId);
        if(ticket.getStatus() == TicketStatusCode.BOUGHT){
            throw new TicketManagerException("Ticket is no longer available");
        }else if(ticket.getStatus() == TicketStatusCode.BUYING){
            throw new TicketManagerException("Ticket is being purchased by another user");
        }else if(ticket.getStatus() == TicketStatusCode.HELD){
            if(ticket.getUserId()!=null && !userId.equals(ticket.getUserId())){
                throw new TicketManagerException("Ticket is held by another user");
            }else{
                return ticket.getHoldTransId();
            }
        }

        global.lock();
        try{
            ticket.setUserId(userId);
            ticket.setStatus(TicketStatusCode.HELD);
            ticket.setHoldTime(System.currentTimeMillis());
            ticket.setHoldTransId(UUID.randomUUID().toString());
        }finally{
            global.unlock();
        }

        update(ticket);

        try{
            heldTickets.put(ticket);
        }catch(InterruptedException e){
            throw new TicketManagerException(e.getMessage());
        }

        return ticket.getHoldTransId();
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
     * @throws TicketManagerException Is thrown if the cancel fails.
     */
    public boolean cancel(@NotNull String userId, @NotNull String ticketId, @NotNull String holdTransId) throws TicketManagerException {
        Ticket ticket = tickets.get(ticketId);
        if(ticket.getStatus()==TicketStatusCode.AVAILABLE && ticket.getHoldTransId() == null){
            return true;
        }else if(ticket.getStatus()==TicketStatusCode.BUYING){
            throw new TicketManagerException("Ticket is being purchased");
        }else if(ticket.getStatus()==TicketStatusCode.BOUGHT){
            throw new TicketManagerException("Ticket is already purchased");
        }
        if(!userId.equals(ticket.getUserId())){
            throw new TicketManagerException("User ID does not match");
        }
        if(!holdTransId.equals(ticket.getHoldTransId())){
            throw new TicketManagerException("Hold Transaction ID does not match");
        }

        global.lock();
        try {
            ticket.setStatus(TicketStatusCode.AVAILABLE);
            ticket.setHoldTransId(null);
            ticket.setUserId(null);
        }finally{
            global.unlock();
        }

        update(ticket);

        return true;
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
        Ticket ticket = tickets.get(ticketId);

        if(ticket.getStatus() == TicketStatusCode.AVAILABLE){
            throw new TicketManagerException("Ticket must first be held");
        }else if(ticket.getStatus() == TicketStatusCode.BOUGHT){
            throw new TicketManagerException("Ticket is already purchased");
        }

        if(!userId.equals(ticket.getUserId())){
            throw new TicketManagerException("User ID does not match");
        }
        if(!holdTransId.equals(ticket.getHoldTransId())){
            throw new TicketManagerException("Hold Transaction ID does not match");
        }

        if(ticket.getStatus()!=TicketStatusCode.BUYING){
            global.lock();
            try{
                ticket.setStatus(TicketStatusCode.BUYING);
            }finally{
                global.unlock();
            }

            update(ticket);

            count.lock();
            try {
                availableTickets--;
            }finally {
                count.unlock();
            }
        }

        Future<String> future = executor.submit(new BuyTask(ticketId, userId));
        String buyId;
        try{
            buyId = future.get();
        }catch(IllegalStateException|ExecutionException e) {
            throw new TicketManagerException("Purchase Failed...");
        }

        if(buyId!=null){
            global.lock();
            try {
                ticket.setStatus(TicketStatusCode.BOUGHT);
                ticket.setBuyTransId(buyId);
            }finally{
                global.unlock();
            }

            update(ticket);

            count.lock();
            try {
                unBoughtTickets--;
                if (unBoughtTickets == 0) {
                    condition.signal();
                }
            }finally {
                count.unlock();
            }
        }

        return buyId;
    }

    /**
     * Blocks until all tickets are in the BOUGHT state.
     * The caller should not shutdown this instance until this method returns.
     * This method is thread-safe.
     *
     * @throws InterruptedException If the thread is interrupted.
     */
    public void awaitAllBought() throws InterruptedException {
        count.lock();
        try {
            if (unBoughtTickets > 0) {
                condition.await();
            }
        }finally{
            count.unlock();
        }
    }

    private void update(@NotNull Ticket ticket) throws TicketManagerException{
        storage_lock.lock();
        try {
            storage.update(ticket);
        }finally {
            storage_lock.unlock();
        }
    }

    private boolean cancel(@NotNull Ticket ticket) throws TicketManagerException{
        return cancel(ticket.getUserId(), ticket.getId(), ticket.getHoldTransId());
    }

    private String buy(@NotNull Ticket ticket) throws TicketManagerException,InterruptedException{
        return buy(ticket.getUserId(), ticket.getId(), ticket.getHoldTransId());
    }

    private class BuyTask implements Callable<String>{

        String ticketId;
        String userId;

        public BuyTask(String ticketId, String userId){
            this.ticketId = ticketId;
            this.userId = userId;
        }

        public String call() throws InterruptedException{
            while(true){
                try {
                    return webservice.buy(ticketId, userId);
                }
                catch (IllegalStateException e) {
                    Thread.sleep(5000);
                }
            }
        }
    }
}
