package edu.umich.clarity;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MPSSim {
	public static final int COMPUTE_SLOTS = 15;
	private BlockingQueue<Query> targetQueries;
	private ArrayList<BlockingQueue<Query>> backgroundQueryTypes;

	private ArrayList<Query> finishedQueries;
	private float global_clock = 0.0f;
	private ArrayList<Query> issuingQueries;
	private BlockingQueue<Kernel> kernelQueue;
	private int global_execution_order = 0;
	private int available_slots = MPSSim.COMPUTE_SLOTS;

	/**
	 * add each type of query into the issuing list at the initial stage.
	 */
	public MPSSim() {
		this.kernelQueue = new LinkedBlockingQueue<Kernel>();
		this.issuingQueries = new ArrayList<Query>();
		issuingQueries.add(targetQueries.poll());
		for (BlockingQueue<Query> backgroundQueries : backgroundQueryTypes) {
			issuingQueries.add(backgroundQueries.poll());
		}
	}

	private void enqueueKernel() {
		ArrayList<Integer> select_range = new ArrayList<Integer>(
				issuingQueries.size());
		for (int i = 0; i < select_range.size(); i++) {
			select_range.add(i);
		}
		while (available_slots >= 0 && select_range.size() != 0) {
			Random random = new Random();
			int chosen_index = random.nextInt(select_range.size());
			int chosen_query = select_range.get(chosen_index);
			Kernel kernel = issuingQueries.get(chosen_query).getKernelQueue()
					.peek();
			if (available_slots - kernel.getOccupancy() >= 0) {
				kernel = issuingQueries.get(chosen_query).getKernelQueue()
						.poll();
				issuingQueries.get(chosen_query).setSeqconstraint(true);
				kernel.setStart_time(global_clock);
				kernelQueue.offer(kernel);
				available_slots -= kernel.getOccupancy();
			}
			select_range.remove(chosen_index);
		}
	}

	public void mps_simulate() {
		// simulate here
		while (issuingQueries.size() != 0) {
			enqueueKernel();
			float longest_execution_time = 0.0f;
			while (!kernelQueue.isEmpty()) {
				Kernel kernel = kernelQueue.poll();
				if (kernel.getRun_time() > longest_execution_time) {
					longest_execution_time = kernel.getRun_time();
				}
			}
			global_clock += longest_execution_time;
		}
	}

	public static void main(String[] args) {

	}
}
