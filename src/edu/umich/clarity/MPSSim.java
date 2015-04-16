package edu.umich.clarity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MPSSim {
	public static final int COMPUTE_SLOTS = 15;
	public static final String PROFILE_PATH = "/home/hailong/git/MPSSIM/bin/edu/umich/clarity/formated/";
	private BlockingQueue<Query> targetQueries;
	private ArrayList<BlockingQueue<Query>> backgroundQueryTypes;

	private ArrayList<Query> finishedQueries;
	private ArrayList<Query> issuingQueries;
	private Queue<Kernel> kernelQueue;
	private boolean pcie_transfer = false;

	private int available_slots = MPSSim.COMPUTE_SLOTS;

	/**
	 * add each type of query into the issuing list at the initial stage.
	 */
	public MPSSim() {
		this.finishedQueries = new ArrayList<Query>();
		this.kernelQueue = new PriorityQueue<Kernel>(100,
				new KernelComparator<Kernel>());
		this.issuingQueries = new ArrayList<Query>();
		this.targetQueries = new LinkedBlockingQueue<Query>();
		this.backgroundQueryTypes = new ArrayList<BlockingQueue<Query>>();
	}

	private void init() {
		// System.out.println("target queue size " + targetQueries.size());
		issuingQueries.add(targetQueries.poll());
		for (BlockingQueue<Query> backgroundQueries : backgroundQueryTypes) {
			issuingQueries.add(backgroundQueries.poll());
		}
		enqueueKernel(0.0f);
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
			// System.out.println("start to simulate...");
			/*
			 * 2. make sure the query selection range within the current issue
			 * list
			 */
			ArrayList<Integer> select_range = new ArrayList<Integer>();
			for (int i = 0; i < issuingQueries.size(); i++) {
				select_range.add(i);
			}
			/*
			 * 3. pick the kernels satisfying the sequential constraints as well
			 * as fitting the computing slots
			 */
			while (available_slots >= 0 && select_range.size() != 0) {
				// System.out.println("select_range size " +
				// select_range.size());
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
					if ((kernel.getOccupancy() != 0)
							|| (kernel.getOccupancy() == 0 && !pcie_transfer)) {
						/*
						 * 6. if all good, get the kernel from selected query
						 * type
						 */
						kernel = issuingQueries.get(chosen_query)
								.getKernelQueue().poll();
						/*
						 * 7. set the kernel's start time and end time
						 */
						issuingQueries.get(chosen_query).setSeqconstraint(true);
						kernel.setStart_time(elapse_time);
						kernel.setEnd_time(kernel.getDuration() + elapse_time);
						kernelQueue.offer(kernel);
						System.out.println("select query "
								+ kernel.getQuery_type() + " kernel order "
								+ kernel.getExecution_order()
								+ " kernel occupacy " + kernel.getOccupancy()
								+ " kernel endtime " + kernel.getEnd_time());
						/*
						 * 8. assign the compute slot for the kernel
						 */
						available_slots -= kernel.getOccupancy();
						if (kernel.getOccupancy() == 0) {
							pcie_transfer = true;
						}
					}
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
		 * initial the simulator to time zero
		 */
		init();
		/*
		 * process the queue while it is not empty
		 */
		while (!kernelQueue.isEmpty()) {
			/*
			 * 1. fetch the queue from queue and execute it (hypothetically)
			 */
			Kernel kernel = kernelQueue.poll();
			/*
			 * 2. mark the kernel as finished and relinquish the pcie bus
			 */
			kernel.setFinished(true);
			if (kernel.getOccupancy() == 0) {
				pcie_transfer = false;
			}
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
				if (kernel.getQuery_type() == 0) {
					if (!targetQueries.isEmpty()) {
						Query comingQuery = targetQueries.poll();
						comingQuery.setStart_time(kernel.getEnd_time());
						issuingQueries.set(kernel.getQuery_type(), comingQuery);
					}
				} else {
					if (!backgroundQueryTypes.get(kernel.getQuery_type() - 1)
							.isEmpty()) {
						Query comingQuery = backgroundQueryTypes.get(
								kernel.getQuery_type() - 1).poll();
						comingQuery.setStart_time(kernel.getEnd_time());
						issuingQueries.set(kernel.getQuery_type(), comingQuery);
					}
				}
			}
			/*
			 * 11. select the kernel to be issued to the queue
			 */
			System.out.println("next issuing cycle....");
			enqueueKernel(kernel.getEnd_time());
		}
	}

	public static void main(String[] args) {
		MPSSim mps_sim = new MPSSim();
		/*
		 * manipulate the input
		 */
		/*
		 * generate the target query
		 */
		Query targetQuery = new Query();
		targetQuery.setQuery_type(Query.TARGET_QUERY);
		targetQuery.setQuery_name("asr");
		BufferedReader fileReader = null;
		String line;
		try {

			fileReader = new BufferedReader(new FileReader(MPSSim.PROFILE_PATH
					+ "asr.csv"));
			// skip the column name of the first line
			line = fileReader.readLine();
			int i = 0;
			while ((line = fileReader.readLine()) != null) {
				String[] profile = line.split(",");
				Kernel kernel = new Kernel();
				kernel.setDuration(new Float(profile[1]).floatValue());
				kernel.setOccupancy(new Integer(profile[4]).intValue());
				kernel.setQuery_type(targetQuery.getQuery_type());
				kernel.setExecution_order(i);
				targetQuery.getKernelQueue().offer(kernel);
				// System.out.println(profile[4]);
				i++;
			}
		} catch (Exception ex) {
			System.out.println("Failed to read the file" + ex.getMessage());
		}
		mps_sim.getTargetQueries().offer(targetQuery);
		/*
		 * generate the background query
		 */
		int bg_query_num = 4;
		for (int i = 0; i < bg_query_num; i++) {
			Query backgroundQuery = new Query();
			backgroundQuery.setQuery_name("stemmer");
			backgroundQuery.setQuery_type(i + 1);
			try {
				fileReader = new BufferedReader(new FileReader(
						MPSSim.PROFILE_PATH + "stemmer.csv"));
				// skip the column name of the first line
				line = fileReader.readLine();
				int k = 0;
				while ((line = fileReader.readLine()) != null) {
					String[] profile = line.split(",");
					Kernel kernel = new Kernel();
					kernel.setDuration(new Float(profile[1]).floatValue());
					kernel.setOccupancy(new Integer(profile[4]).intValue());
					kernel.setQuery_type(backgroundQuery.getQuery_type());
					kernel.setExecution_order(k);
					backgroundQuery.getKernelQueue().offer(kernel);
					k++;
					// System.out.println(profile[1]);
				}
			} catch (Exception ex) {
				System.out.println("Failed to read the file" + ex.getMessage());
			}
			mps_sim.getBackgroundQueryTypes().add(
					new LinkedBlockingQueue<Query>());
			mps_sim.getBackgroundQueryTypes().get(i).offer(backgroundQuery);
		}
		/*
		 * start to simulate
		 */
		mps_sim.mps_simulate();
		/*
		 * print the results from the finished query queue
		 */
		for (Query finishedQuery : mps_sim.getFinishedQueries()) {
			System.out.println(finishedQuery.getEnd_time()
					- finishedQuery.getStart_time());
		}
	}

	public BlockingQueue<Query> getTargetQueries() {
		return targetQueries;
	}

	public void setTargetQueries(BlockingQueue<Query> targetQueries) {
		this.targetQueries = targetQueries;
	}

	public ArrayList<BlockingQueue<Query>> getBackgroundQueryTypes() {
		return backgroundQueryTypes;
	}

	public void setBackgroundQueryTypes(
			ArrayList<BlockingQueue<Query>> backgroundQueryTypes) {
		this.backgroundQueryTypes = backgroundQueryTypes;
	}

	public ArrayList<Query> getFinishedQueries() {
		return finishedQueries;
	}

	public void setFinishedQueries(ArrayList<Query> finishedQueries) {
		this.finishedQueries = finishedQueries;
	}

}
