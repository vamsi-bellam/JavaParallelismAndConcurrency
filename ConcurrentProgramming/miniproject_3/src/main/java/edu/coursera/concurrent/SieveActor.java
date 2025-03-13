package edu.coursera.concurrent;

import edu.rice.pcdp.Actor;

import java.util.ArrayList;
import java.util.List;

import static edu.rice.pcdp.PCDP.finish;

/**
 * An actor-based implementation of the Sieve of Eratosthenes.
 * <p>
 * TODO Fill in the empty SieveActorActor actor class below and use it from
 * countPrimes to determin the number of primes <= limit.
 */
public final class SieveActor extends Sieve {
    /**
     * {@inheritDoc}
     * <p>
     * TODO Use the SieveActorActor class to calculate the number of primes <=
     * limit in parallel. You might consider how you can model the Sieve of
     * Eratosthenes as a pipeline of actors, each corresponding to a single
     * prime number.
     */
    @Override
    public int countPrimes(final int limit) {
        final SieveActorActor sa = new SieveActorActor(2);
        finish(() -> {
            for (int i = 3; i <= limit; i += 2) {
                sa.send(i);
            }
        });

        int count = 0;
        SieveActorActor la = sa;
        while (la != null) {
            count += la.countLocalPrimes();
            la = la.next();
        }
        return count;
    }

    /**
     * An actor class that helps implement the Sieve of Eratosthenes in
     * parallel.
     */
    public static final class SieveActorActor extends Actor {
        private SieveActorActor nextActor;
        private final List<Integer> localPrimes;
        private final int MAX = 5_000;

        public SieveActorActor(int start) {
            localPrimes = new ArrayList<>();
            localPrimes.add(start);
            nextActor = null;
        }

        public int countLocalPrimes() {
            return localPrimes.size();
        }

        public SieveActorActor next() {
            return nextActor;
        }

        /**
         * Process a single message sent to this actor.
         * <p>
         * TODO complete this method.
         *
         * @param msg Received message
         */
        @Override
        public void process(final Object msg) {
            int num = (Integer) msg;
            boolean isPrime = checkPrime(num, localPrimes);
            if (isPrime) {
                if (this.countLocalPrimes() < MAX) {
                    localPrimes.add(num);
                } else if (nextActor != null) {
                    nextActor.send(msg);
                } else {
                    nextActor = new SieveActorActor(num);
                }
            }
        }

        private boolean checkPrime(final int candidate,
                                   final List<Integer> primesList) {
            boolean isPrime = true;
            final int s = primesList.size();
            for (Integer integer : primesList) {
                if (candidate % integer == 0) {
                    isPrime = false;
                    break;
                }
            }
            return isPrime;
        }
    }
}
