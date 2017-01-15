package com.SeimiCrawlerDemo;

import java.util.ArrayList;

public class ProgressBarThread implements Runnable {

	private ArrayList<Integer> proList = new ArrayList<Integer>();
	private int progress;// 当前进度
	private int totalSize;// 总大小
	private boolean run = true;

	public ProgressBarThread(int totalSize) {
		this.totalSize = totalSize;
	}

	/**
	 * @param progress
	 *            进度
	 */
	public void updateProgress(int progress) {
		synchronized (this.proList) {
			if (this.run) {
				this.proList.add(progress);
				this.proList.notify();
			}
		}
	}

	public void finish() {
		this.run = false;
	}

	@Override
	public void run() {
		synchronized (this.proList) {
			try {
				while (this.run) {
					if (this.proList.size() == 0) {
						this.proList.wait();
					}
					synchronized (proList) {
						this.progress += this.proList.remove(0);
						//更新进度条
						System.err.println("当前进度：" + (this.progress / this.totalSize * 100) + "%");
					}
				}
				System.err.println("下载完成");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
