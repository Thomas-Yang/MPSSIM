package edu.umich.clarity;

public class Kernel {
	private String kernel_name;
	private int query_type;
	private float start_time;
	private float end_time;
	private float duration;
	private int occupancy;
	private boolean finished;

	public Kernel() {
		this.finished = false;
	}

	public String getKernel_name() {
		return kernel_name;
	}

	public void setKernel_name(String kernel_name) {
		this.kernel_name = kernel_name;
	}

	public int getQuery_type() {
		return query_type;
	}

	public void setQuery_type(int query_type) {
		this.query_type = query_type;
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

	public float getDuration() {
		return duration;
	}

	public void setDuration(float duration) {
		this.duration = duration;
	}

	public int getOccupancy() {
		return occupancy;
	}

	public void setOccupancy(int occupancy) {
		this.occupancy = occupancy;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
}