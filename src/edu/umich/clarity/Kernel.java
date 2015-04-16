package edu.umich.clarity;

public class Kernel {
	public String kernel_name;
	public String query_type;
	public float start_time;
	public float end_time;
	public float run_time;
	public int occupancy;
	public int execution_order;
	public boolean finished;

	public String getKernel_name() {
		return kernel_name;
	}

	public void setKernel_name(String kernel_name) {
		this.kernel_name = kernel_name;
	}

	public float getStart_time() {
		return start_time;
	}

	public void setStart_time(float start_time) {
		this.start_time = start_time;
	}

	public float getEnd_time() {
		return end_time;
	}

	public void setEnd_time(float end_time) {
		this.end_time = end_time;
	}

	public float getRun_time() {
		return run_time;
	}

	public void setRun_time(float run_time) {
		this.run_time = run_time;
	}

	public int getOccupancy() {
		return occupancy;
	}

	public void setOccupancy(int occupancy) {
		this.occupancy = occupancy;
	}

	public int getExecution_order() {
		return execution_order;
	}

	public void setExecution_order(int execution_order) {
		this.execution_order = execution_order;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
}