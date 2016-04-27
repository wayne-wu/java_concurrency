Java Training Project 2
=======================

In this project, you will create a multi-threaded library for buying
tickets. Some of the things you will learn, include:

* how to create concurrent tasks using Executors.
* how to execute a task periodically.
* how to use locks to protect data that is accessed by multiple threads.
* testing multi-threaded code with a multi-threaded test.
* when building a new feature, it is good practice to first work out
  the interfaces before starting to write code. This project has already
  defined the library's interface which demonstrates the necessary detail
  needed for a quality implementation. We expect that you do the same
  for any internal interfaces (i.e. classes) in your implementation.

### Submission

To do this exercise, first fork the project. Then when ready to submit,
send a PR.

You are expected to submit a production-quality program. It must be
fully documented and formatted using the company's standard Java
coding conventions, which is defined [here](http://yo/javastandard)
with one exception: the indent is 4 spaces rather than 2. The
standards group is working on config files for the IDE's and build
tools. For now, using shift-control-F in Eclipse or option-cmd-L in
Intellij is good enough. We
intend to do a pretty strict review on both the code and the
documentation to ensure that all participants learn the best
practices.

### Functional Requirements

Create a library to facilitate the purchase of tickets.
The library should be able to handle the purchase of up to 100,000
tickets. Unfortunately, the tickets are stored in a system that
provides no transactions and cannot be accessed concurrently.
Therefore the library must properly synchronize all access to
the storage system.

When a user purchases a ticket, the library should update the status
of the ticket in the storage system and call a special webservice to
purchase the ticket (which, in real life, would
validate the user and send a confirmation email).
The webservice is somewhat slow but fortunately, can be called
with up to 5 concurrent requests. The library should take advantage
of this feature and minimize latency by creating multiple
threads to call the webservice.

Tickets can be in one of 4 states:
* Available - the ticket can be put on hold.
* Held - the ticket is being held by a user. In this state, the ticket
  can either be cancelled or purchased by the user. If the user does
  neither within a certain period of time, the library must 
  automatically cancel the hold status and make the ticket available
  again.
* Buying - the ticket is in the process of being purchased. In this 
  state, the status of the ticket has been updated in storage. Once the
  storage update succeeds, a request is sent to the webservice to
  complete the purchase.
* Bought - once, the request to the webservice has succeeded, the ticket
  will be set to this state.

The library will be given the following input:
* An instance to the ticket storage system.
* An instance of a webservice for buying the tickets.
* An expiration time. If a ticket is held longer than this expiration
time, the ticket will be automatically cancelled.


### Implementation Requirements

* A lot of code has already been provided for you. Your job is to
  implement the TicketManager class and get the unit and integration
  test to pass.
* Use of the synchronized keyword is not allowed. The
  java.util.concurrent library has everything you need to deal with
  multi-threaded coding.
* With the latest version of Java, there's less need to create
  Thread objects. For this exercise, use the
  java.util.conccurrent.Executors class instead
  defining Thread objects.
* Use Storage.getTickets() to load all the tickets into memory and
  use a global lock to synchronize the state changes to the tickets and
  to calls to the Storage class (since none of the Storage 
  methods are thread-safe).
* When calling the buy webservice, you MUST not wait for the results
  while holding the global lock. This would block all other ticket 
  change requests and kill your concurrency. Release the lock before
  calling the service and then reacquire the lock after the service
  call returns.
* When starting up, if there are any tickets in the BUYING state,
  finish the purchase of those tickets by calling the webservice.
* Tickets must be expired in less than 2 times the specified expiration
  period. E.g. if the expiration time is 10 minutes, then the expiration
  must happen within 20 minutes.
* To generate a transaction id, use UUID.randomUUID().toString().

### TicketManager Interface

Here's a quick overview of the class that you need to implement:

```
public class TicketManager {
    public TicketManager(long expireTimeMs, Storage storage, WebService webservice)

    // Returns the number of tickets still available for purchase
    public int availableCount()

    // Holds a ticket, preventing someone else from purchasing it
    public String hold(String userId, String ticketId)

    // Cancels a held ticket
    public void cancel(String userId, String ticketId, String holdTransId)

    // Buys a ticket
    public String buy(String userId, String ticketId, String holdTransId)

    // Returns when all tickets are sold
    public void awaitSoldOut()

    // Does an orderly shutdown of all the threads
    public void shutdown()
}
```

### Tutorials

Here are some links to some of the concepts and libraries that you will need to complete this exercise:

* [Locks and Condition Variables](http://www.math.uni-hamburg.de/doc/java/tutorial/essential/threads/explicitlocks.html)
* [Executor Service](http://tutorials.jenkov.com/java-util-concurrent/executorservice.html)
* [Java 8 Concurrency Intro](http://winterbe.com/posts/2015/04/07/java8-concurrency-tutorial-thread-executor-examples/)
* [Java Tutorial: Concurrency](https://docs.oracle.com/javase/tutorial/essential/concurrency/)


### Unit Tests

Unit tests have been developed to test your implementation. Type

```
mvn test
```

### Concurrent User Tests

Multi-threaded code is hard to test. Unit tests are not enough because it
impossible to test for every possible sequence of actions.
Short of a formal proof, the best known way to find as many concurrency bugs
as possible is to build a test where the longer you run it, the higher
the probabilty that more race conditions have been tested.

A concurrency test has been developed to test your
implementation:

```
mvn package    # to build the test
bin/concurrent_user_tester
```

This test will create a file of 1000 tickets and 50 users that will
perform random actions concurrently. The test ends when all the 
tickets have been purchased.
