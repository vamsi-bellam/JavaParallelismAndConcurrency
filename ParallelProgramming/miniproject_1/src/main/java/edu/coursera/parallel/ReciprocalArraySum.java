package edu.coursera.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Class wrapping methods for implementing reciprocal array sum in parallel.
 */
public final class ReciprocalArraySum {

    /**
     * Default constructor.
     */
    private ReciprocalArraySum() {
    }

    /**
     * Sequentially compute the sum of the reciprocal values for a given array.
     *
     * @param input Input array
     * @return The sum of the reciprocals of the array input
     */
    protected static double seqArraySum(final double[] input) {
        double sum = 0;

        // Compute sum of reciprocals of array elements
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }

        return sum;
    }

    /**
     * Computes the size of each chunk, given the number of chunks to create
     * across a given number of elements.
     *
     * @param nChunks   The number of chunks to create
     * @param nElements The number of elements to chunk across
     * @return The default chunk size
     */
    private static int getChunkSize(final int nChunks, final int nElements) {
        // Integer ceil
        return (nElements + nChunks - 1) / nChunks;
    }

    /**
     * Computes the inclusive element index that the provided chunk starts at,
     * given there are a certain number of chunks.
     *
     * @param chunk     The chunk to compute the start of
     * @param nChunks   The number of chunks created
     * @param nElements The number of elements to chunk across
     * @return The inclusive index that this chunk starts at in the set of
     * nElements
     */
    private static int getChunkStartInclusive(final int chunk, final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        return chunk * chunkSize;
    }

    /**
     * Computes the exclusive element index that the provided chunk ends at,
     * given there are a certain number of chunks.
     *
     * @param chunk     The chunk to compute the end of
     * @param nChunks   The number of chunks created
     * @param nElements The number of elements to chunk across
     * @return The exclusive end index for this chunk
     */
    private static int getChunkEndExclusive(final int chunk, final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        final int end = (chunk + 1) * chunkSize;
        if (end > nElements) {
            return nElements;
        } else {
            return end;
        }
    }

    /**
     * This class stub can be filled in to implement the body of each task
     * created to perform reciprocal array sum in parallel.
     */
    private static class ReciprocalArraySumTask extends RecursiveAction {
        /**
         * Starting index for traversal done by this task.
         */
        private final int startIndexInclusive;
        /**
         * Ending index for traversal done by this task.
         */
        private final int endIndexExclusive;
        /**
         * Input array to reciprocal sum.
         */
        private final double[] input;
        /**
         * Intermediate value produced by this task.
         */
        private double value;

        /**
         * Constructor.
         *
         * @param setStartIndexInclusive Set the starting index to begin
         *                               parallel traversal at.
         * @param setEndIndexExclusive   Set ending index for parallel traversal.
         * @param setInput               Input values
         */
        ReciprocalArraySumTask(final int setStartIndexInclusive, final int setEndIndexExclusive, final double[] setInput) {
            this.startIndexInclusive = setStartIndexInclusive;
            this.endIndexExclusive = setEndIndexExclusive;
            this.input = setInput;
        }

        /**
         * Getter for the value produced by this task.
         *
         * @return Value produced by this task
         */
        public double getValue() {
            return value;
        }

        @Override
        protected void compute() {
            value = 0;
            if (startIndexInclusive >= endIndexExclusive) return;

            // Basic Idea - If size is below some threshold use sequential computation else
            // split to half and parallelize using fork and join

            // This threshold was making much of the difference when input is high.
            // Need to find a value that shouldn't add bottleneck of creating too many tasks.
            // Even this value looks not perfect - two hundred million input tests is sometimes flaky!

            if (endIndexExclusive - startIndexInclusive <= 100_000) {
                for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                    value += 1 / input[i];
                }
            } else {
                int mid = (endIndexExclusive + startIndexInclusive) / 2;
                ReciprocalArraySumTask left = new ReciprocalArraySumTask(startIndexInclusive, mid, input);
                ReciprocalArraySumTask right = new ReciprocalArraySumTask(mid, endIndexExclusive, input);
                left.fork();
                right.compute();
                left.join();
                /*
                 invokeAll(left, right);

                 * Notes : invokeAll(..tasks) also do the similar job of fork -> compute -> join,
                 * But it waits for all given tasks to complete and no specific order on how they execute
                 * Ideal for multiple independent tasks to go to pool.
                 * If there is specific ordering on how tasks should be done, better to control it using fork and join
                 */
                value = left.getValue() + right.getValue();
            }

        }
    }

    private static Double compute(ReciprocalArraySumTask rast) {
        rast.compute();
        return rast.getValue();
    }

    /**
     * TODO: Modify this method to compute the same reciprocal sum as
     * seqArraySum, but use two tasks running in parallel under the Java Fork
     * Join framework. You may assume that the length of the input array is
     * evenly divisible by 2.
     *
     * @param input Input array
     * @return The sum of the reciprocals of the array input
     */
    protected static double parArraySum(final double[] input) {
        assert input.length % 2 == 0;
        return parManyTaskArraySum(input, 2);
    }

    /**
     * TODO: Extend the work you did to implement parArraySum to use a set
     * number of tasks to compute the reciprocal array sum. You may find the
     * above utilities getChunkStartInclusive and getChunkEndExclusive helpful
     * in computing the range of element indices that belong to each chunk.
     *
     * @param input    Input array
     * @param numTasks The number of tasks to create
     * @return The sum of the reciprocals of the array input
     */
    protected static double parManyTaskArraySum(final double[] input, final int numTasks) {

        /* Initially used ForkJoinPool (with both setting up parallelism manually and using common pool)
         *  For the invokeAll we need to pass tasks that are `Callable` so, wrapping them up in lambda functions
         * was slow and found to be a bit over head.
         * */

        List<ReciprocalArraySumTask> tasks = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            tasks.add(new ReciprocalArraySumTask(getChunkStartInclusive(i, numTasks, input.length), getChunkEndExclusive(i, numTasks, input.length), input));
        }

        // one more imp note - if executed outside of pool -> invokeAll executes tasks in seq only
        // But surprisingly performs same as below which executes invokeAll in a pool
        // Need to understand this more
        //ForkJoinTask.invokeAll(tasks);

//        ForkJoinPool.commonPool().invoke(new RecursiveAction() {
//            @Override
//            protected void compute() {
//                invokeAll(tasks);
//            }
//        });


//        double sum = 0;
//        for (ReciprocalArraySumTask task : tasks) {
//            sum += task.getValue();
//        }
//        return sum;

        // Uses current pool to distribute forks among threads
        for (ReciprocalArraySumTask task : tasks) {
            task.fork();
        }

        for (ReciprocalArraySumTask task : tasks) {
            task.join();
        }

        double sum = 0;
        for (ReciprocalArraySumTask task : tasks) {
            sum += task.getValue();
        }
        return sum;

    }
}
