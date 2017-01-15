package com.download;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * 速度监控、限制，1秒内的平均下载
 */
public class DLSpeedMonitor {
	private static final int PERIOD = 1000000000;// nano
	private LinkedList<Speed> speedList = new LinkedList<Speed>();
	private float sleepTime = 10;
	private int limit;
	
	public void setLimit(int bytes) {
		this.limit = bytes;
	}
	
	public int getLimit() {
		return limit;
	}
	
	public synchronized void update(int bytes) {
		speedList.add(new Speed(bytes));
		
		long begin = System.nanoTime() - PERIOD;
		
		for (Speed speed : new ArrayList<Speed>(speedList)) {
			if(speed.nano >= begin) {
				break;
			}
			speedList.remove(0);
		}

		if(limit > 0) {
			speedLimit();
		}
	}

	/**
	 * 限速
	 */
	private void speedLimit() {
		int speed = getSpeed();
		
		if(speed > limit ) {
			sleepTime += 0.1;//加减得越小波动越小
		} else if(sleepTime > 1) {
			sleepTime -= 0.05;
		}
		
		if(sleepTime >= 1)
			try {
				Thread.sleep((long) sleepTime);
			} catch (InterruptedException e) {
			}
	}
	
	public synchronized int getSpeed() {
		int total = 0;
		long begin = System.nanoTime() - PERIOD;
		for (Speed speed : speedList) {
			if(speed.nano >= begin)
				total += speed.bytes;
		}
		return (int) ((double)total / PERIOD * 1000000000);
	}
	
	public float getSleepTime() {
		return sleepTime;
	}

	class Speed {
		final long nano;
		final int bytes;
		Speed(int bytes) {
			this.nano = System.nanoTime();
			this.bytes = bytes;
		}
	}
}
