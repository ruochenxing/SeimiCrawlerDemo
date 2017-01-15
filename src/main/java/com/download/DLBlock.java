package com.download;

import com.alibaba.fastjson.annotation.JSONField;
/**
 * 下载块（将任务分成几块下载）
 */
public class DLBlock {

		public static final String[] STATUS_STR = {"暂停","下载中","完成"};
		public static final int STATUS_PAUSE = 0;//暂停
		public static final int STATUS_RUNNING = 1;//下载
		public static final int STATUS_DONE = 2;//完成
		
		private long begin;
		private long end;
		private long downloaded;
		private transient Throwable throwable;
		private int status;
		private DLTask task;
		private DLCallback<DLBlock> onStatusChange;
		private DLCallback<DLBlock> onDownloadChange;
		
		private transient DLSpeedMonitor speedMonitor = new DLSpeedMonitor();
		
		public DLBlock() {
		}
		
		public DLBlock(DLTask task, DLCallback<DLBlock> onBlockStatusChange, DLCallback<DLBlock> onBlockDownloadChange, long begin, long end) {
			this.task = task;
			this.begin = begin;
			this.end = end;
			this.onStatusChange = onBlockStatusChange;
			this.onDownloadChange = onBlockDownloadChange;
		}
		
		public void setOnDownloadChange(DLCallback<DLBlock> onDownloadChange) {
			this.onDownloadChange = onDownloadChange;
		}
		
		public void setOnStatusChange(DLCallback<DLBlock> onStatusChange) {
			this.onStatusChange = onStatusChange;
		}
		
		@JSONField(serialize=false)
		public DLTask getTask() {
			return task;
		}
		
		public void setTask(DLTask task) {
			this.task = task;
		}
		
		public long getBegin() {
			return begin;
		}
		public void setBegin(long begin) {
			this.begin = begin;
		}
		public long getEnd() {
			return end;
		}
		public void setEnd(long end) {
			this.end = end;
		}
		
		public void setStatus(int status) {
			this.status = status;
			DLCallback.trigger(onStatusChange, this);
		}

		public int getStatus() {
			return status;
		}
		
		public void setDownloaded(long downloaded) {
			this.downloaded = downloaded;
		}
		
		public long getDownloaded() {
			return downloaded;
		}
		
		@JSONField(serialize=false)
		public void setThrowable(Throwable throwable) {
			this.throwable = throwable;
			setStatus(STATUS_PAUSE);
		}
		
		public Throwable getThrowable() {
			return throwable;
		}

		@JSONField(serialize=false)
		public boolean isDone() {
			return status == STATUS_DONE;
		}

		@JSONField(serialize=false)
		public boolean isPause() {
			return status == STATUS_PAUSE;
		}

		@JSONField(serialize=false)
		public boolean isRunning() {
			return status == STATUS_RUNNING;
		}

		public void updateDownloaded(int size) {
			this.downloaded += size;
			speedMonitor.update(size);
			DLCallback.trigger(onDownloadChange, this);
		}
		
		@JSONField(serialize=false)
		public DLSpeedMonitor getSpeedMonitor() {
			return speedMonitor;
		}
		
		@Override
		public String toString() {
			return task.getTaskId() + ": " +DLUtils.getLengthStr(begin) + "-" + DLUtils.getLengthStr(end) + "|" + DLUtils.getLengthStr(downloaded) + " " + DLUtils.getPercentStr(downloaded, end-begin) + "|" + DLUtils.getLengthStr(speedMonitor.getSpeed()) + "/s|" + STATUS_STR[status];
		}
	}