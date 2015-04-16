package edu.umich.clarity;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public class MPSSim {
	public static final int COMPUTE_SLOTS = 15;
	private BlockingQueue<Query> targetQueries;
	private ArrayList<BlockingQueue<Query>> backgroundQueryTypes;

	private ArrayList<Query> finishedQueries;
	private ArrayList<Query> issuingQueries;
	private Queue<Kernel> kernelQueue;

	private int available_slots = MPSSim.COMPUTE_SLOTS;

	/**
	 * add each type of query into the issuing list at the initial stage.
	 */
	public MPSSim() {
		this.finishedQueries = new ArrayList<Query>(targetQueries.size());
		this.kernelQueue = new PriorityQueue<Kernel>(100,
				new KernelComparator<Kernel>());
		this.issuingQueries = new ArrayList<Query>();
		issuingQueries.add(targetQueries.poll());
		for (BlockingQueue<Query> backgroundQueries : backgroundQueryTypes) {
			issuingQueries.add(backgroundQueries.poll());
		}
	}

	/**
	 * Select the right kernel to be issued to the kernel queue. A couple of
	 * constraints need to be met in order to mimic the real experiment setup.
	 * 1.kernel within the same type of query should be issued sequentially 2.
	 * kernels from different types of queries can be executed concurrently as
	 * long as their accumulated occupancy not exceeds the resource threshold.
	 * 3. unless the running queries have been finished, the same type of
	 * queries cannot be issued
	 * 
	 * @param elapse_time
	 *            the elapsed time since simulation starts
	 */
	private void enqueueKernel(float elapse_time) {
		/*
		 * 1. if the issue list is empty, then all the queries at least have
		 * been issued for processing
		 */
		if (issuingQueries.size() != 0) {
			/*
			 * 2. make sure the query selection range within the current issue
			 * list
			 */
			ArrayList<Integer> select_range = new ArrayList<Integer>(
					issuingQueries.size());
			for (int i = 0; i < select_range.size(); i++) {
				select_range.add(i);
			}
			/*
			 * 3. pick the kernels satisfying the sequential constraints as well
			 * as fitting the computing slots
			 */
			while (available_slots >= 0 && select_range.size() != 0) {
				/*
				 * 4. random select one query candidate to mimic FIFO within MPS
				 */
				Random random = new Random();
				int chosen_index = random.nextInt(select_range.size());
				int chosen_query = select_range.get(chosen_index);
				/*
				 * 5. test whether the candidate meet the requirements
				 */
				Kernel kernel = issuingQueries.get(chosen_query)
						.getKernelQueue().peek();
				boolean isConstrained = issuingQueries.get(chosen_query)
						.isSeqconstraint();
				if (!isConstrained
						&& available_slots - kernel.getOccupancy() >= 0) {
					/*
					 * 6. if all good, get the kernel from selected query type
					 */
					kernel = issuingQueries.get(chosen_query).getKernelQueue()
							.poll();
					/*
					 * 7. set the kernel's start time and end time
					 */
					issuingQueries.get(chosen_query).setSeqconstraint(true);
					kernel.setStart_time(elapse_time);
					kernel.setEnd_time(kernel.getDuration() + elapse_time);
					kernelQueue.offer(kernel);
					/*
					 * 8. assign the compute slot for the kernel
					 */
					available_slots -= kernel.getOccupancy();
				}
				/*
				 * 9. this type of query should not be considered during next
				 * round
				 */
				select_range.remove(chosen_index);
			}
		}
	}

	public void mps_simulate() {
		/*
		 * simulate here populate the queue at time zero
		 */
		enqueueKernel(0.0f);
		/*
		 * process the queue while it is not empty
		 */
		while (!kernelQueue.isEmpty()) {
			/*
			 * 1. fetch the queue from queue and execute it (hypothetically)
			 */
			Kernel kernel = kernelQueue.poll();
			/*
			 * 2. mark the kernel as finished
			 */
			kernel.setFinished(true);
			/*
			 * 3. mark the query as not sequential constrained to issue kernel
			 */
			issuingQueries.get(kernel.getQuery_type()).setSeqconstraint(false);
			/*
			 * 4. relinquish the computing slots back to the pool
			 */
			available_slots += kernel.getOccupancy();
			/*
			 * 5. add the finished kernel back to the query's finished kernel
			 * queue
			 */
			issuingQueries.get(kernel.getQuery_type()).getFinishedKernelQueue()
					.offer(kernel);
			/*
			 * 6. if all the kernels from the query have been finished
			 */
			if (issuingQueries.get(kernel.getQuery_type()).getKernelQueue()
					.isEmpty()) {
				/*
				 * 7. set the finish time (global time) of the query
				 */
				issuingQueries.get(kernel.getQuery_type()).setEnd_time(
						kernel.getEnd_time());
				Query query = issuingQueries.get(kernel.getQuery_type());
				// remove the finished query from the issue list
				/*
				 * 8. if the target query, save the finished query to a list
				 */
				if (query.getQuery_type() == Query.TARGET_QUERY) {
					finishedQueries.add(query);
				}
				/*
				 * 9. remove the finished query from the issue list
				 */
				issuingQueries.remove(kernel.getQuery_type());
				/*
				 * 10. add the same type of query to the issue list unless the
				 * query queue is empty for that type of query
				 */
				if (!backgroundQueryTypes.get(kernel.getQuery_type() - 1)
						.isEmpty()) {
					Query comingQuery = backgroundQueryTypes.get(
							kernel.getQuery_type() - 1).poll();
					comingQuery.setStart_time(kernel.getEnd_time());
					issuingQueries.set(kernel.getQuery_type(), comingQuery);
				}
			}
			/*
			 * 11. select the kernel to be issued to the queue
			 */
			enqueueKernel(kernel.getEnd_time());
		}
	}

	public static void main(String[] args) {

		/*
		 * manipulate the input
		 */
		
		/*
		 * start to simulate
		 */
		MPSSim mps_sim = new MPSSim();
		mps_sim.mps_simulate();
		/*
		 * print the results from the finished query queue
		 */

	}
}
