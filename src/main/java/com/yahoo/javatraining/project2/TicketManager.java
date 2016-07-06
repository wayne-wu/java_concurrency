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
    private long expireTimeMs;
    private WebService webservice;
    private boolean isShutDown;
    private int availableTickets;

    private ExecutorService executor; //for executing webservice requests
    private ScheduledExecutorService timer; //for timing and resetting expired held tickets
    private ScheduledExecutorService monitor; //for monitoring the quantities
    private BlockingQueue<Ticket> heldTickets;
    private HashMap<String, Ticket> tickets;
    private Lock global = new ReentrantLock();
    private Lock storage_lock = new ReentrantLock();
    private Condition condition = global.newCondition();

    /**
     * Constructs a ticket manager
     *
     * @param storage    A storage instance for storing updates to the tickets.
     * @param webservice A service instance to use for purchases.
     */
    public TicketManager(long expireTimeMs, @NotNull Storage storage, @NotNull WebService webservice)
            throws TicketManagerException {
        this.storage = storage;
        this.expireTimeMs = expireTimeMs;
        this.webservice = webservice;
        this.isShutDown = false;
        this.executor = Executors.newFixedThreadPool(5);
        this.timer = Executors.newScheduledThreadPool(1);
        this.heldTickets = new LinkedBlockingQueue<>();
        this.tickets = new HashMap<>();
        for(Ticket tik : storage.getTickets()){
            tickets.put(tik.getId(), tik);
        }

        Runnable resetTask = () -> {
            try{
                Ticket ticket = heldTickets.peek();
                if(ticket.getStatus() != TicketStatusCode.HELD){
                    heldTickets.take(); //no longer held
                }
                if((System.currentTimeMillis()-ticket.getBuyingTime())>expireTimeMs){
                    cancel(heldTickets.take());
                }
            }catch(TicketManagerException|InterruptedException e){
                throw new IllegalStateException();
            }
        };

        long period = (expireTimeMs<10000) ? expireTimeMs/10 : 1000;
        timer.scheduleAtFixedRate(resetTask, expireTimeMs, period, TimeUnit.MILLISECONDS);
        //PSEUDO: Complete all BUYING tickets
        //use executor to run the task
    }

    /**
     * Rejects further calls to this class and shutdowns on-going concurrent tasks.
     * The object is no longer usable after this call.
     *
     * @throws InterruptedException If the shutdown was interrupted.
     */
    public void shutdown() throws InterruptedException {
        //Shut down all tasks
        isShutDown = true;
        executor.shutdown();
        timer.shutdown();
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
        if(availableTickets==0){
            condition.signal();
            return availableTickets;
        }else{
            return availableTickets;
        }
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
        if(ticket.getUserId()!=null && !userId.equals(ticket.getUserId())){
            throw new TicketManagerException("Ticket is held by another user");
        }
        if(ticket.getStatus() == TicketStatusCode.HELD){
            return ticket.getHoldTransId();
        }

        global.lock();
        ticket.setUserId(userId);
        ticket.setStatus(TicketStatusCode.HELD);
        ticket.setHoldTime(System.currentTimeMillis());
        String transId = UUID.randomUUID().toString();
        ticket.setHoldTransId(transId);
        global.unlock();

        storage_lock.lock();
        storage.update(ticket);
        storage_lock.unlock();

        try{
            heldTickets.put(ticket);
            System.out.println("Added to queue");
        }catch(InterruptedException e){
            throw new TicketManagerException(e.getMessage());
        }

        return transId;
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

        if(ticket.getHoldTransId()==null){
            return true;
        }
        if(!holdTransId.equals(ticket.getHoldTransId()) || !userId.equals(ticket.getUserId())){
            throw new TicketManagerException("Hold Transaction Id or User Id does not match");
        }

        global.lock();
        ticket.setStatus(TicketStatusCode.AVAILABLE);
        ticket.setHoldTransId(null);
        ticket.setUserId(null);
        global.unlock();

        storage_lock.lock();
        storage.update(ticket);
        storage_lock.unlock();

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
        if(!userId.equals(ticket.getUserId()) || !holdTransId.equals(ticket.getHoldTransId())){
            throw new TicketManagerException("User id or Hold transaction id does not match");
        }

        global.lock();
        ticket.setStatus(TicketStatusCode.BUYING);
        availableTickets--;
        global.unlock();

        storage_lock.lock();
        storage.update(ticket);
        storage_lock.unlock();

        Future<String> future = executor.submit(() -> {
            while(true){
                try {
                    return webservice.buy(ticketId, userId);
                }
                catch (IllegalStateException e) {
                    Thread.sleep(5000);
                }
            }
        });
        String buyId = null;
        try{
            buyId = future.get();
        }catch(IllegalStateException|ExecutionException e) {
            throw new TicketManagerException(e.getMessage());
        }finally{
            global.lock();
            ticket.setStatus(TicketStatusCode.BOUGHT);
            ticket.setBuyTransId(buyId);
            global.unlock();
        }
        storage_lock.lock();
        storage.update(ticket);
        storage_lock.unlock();

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
        condition.await();
        shutdown();
    }


    public boolean cancel(@NotNull Ticket ticket) throws TicketManagerException{
        return cancel(ticket.getUserId(), ticket.getId(), ticket.getHoldTransId());
    }
}
