package com.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 下载任务
 */
public class DLTask {
	
	private static int TASK_ID = 1;

	public static final String[] STATUS_STR = {"暂停","下载中","完成","等待","删除"};
	
	public static final int STATUS_PAUSE = 0;//暂停
	public static final int STATUS_RUNNING = 1;//下载
	public static final int STATUS_DONE = 2;//完成
	public static final int STATUS_WATING = 3;//等待下载
	public static final int STATUS_DEL = 4;//删除

	private long length;// 文件长度
	private String name;
	private String uri;// 文件地址
	private String file;//保存地址
	private String tmpfile;//临时地址
	private boolean isSupportedContentRange;// url是否支持断点续传
	private List<DLBlock> blocks;//分块下载,对其迭代时需要对this(DLTask)加锁
	private int status;//状态
	private transient int taskId = 0;//任务ID
	private String saveDir;//保存目录
	private transient List<DLThread> threads = new ArrayList<DLThread>();//下载线程, 对其迭代时需要对this(DLTask)加锁

	private DLCallback<DLTask> onStatusChange;
	private DLCallback<DLTask> onThreadsShutdown;

	private int limit;//限速
	
	public DLTask() {
		this.taskId = TASK_ID++;
	}

	public DLTask(DLCallback<DLTask> onStatusChange, String uri, String name, String saveDir, long length, boolean isSupportedContentRange) throws IOException {
		this();
		this.onStatusChange = onStatusChange;
		this.uri = uri;
		this.name = name;
		this.saveDir = saveDir;
		if(!saveDir.endsWith("/")){
			this.file = saveDir + "/" + name;
		}
		else{
			this.file = saveDir + name;
		}
		this.length = length;
		this.isSupportedContentRange = isSupportedContentRange;
	}
	
	public void setOnStatusChange(DLCallback<DLTask> onStatusChange) {
		this.onStatusChange = onStatusChange;
	}

	/**
	 * 创建空白文件并分配磁盘空间
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void makeFile() throws FileNotFoundException, IOException {
		if(tmpfile != null) {
			throw new RuntimeException("This method can only be called once!");
		}
		
		file = DLUtils.checkFileAndRename(file);
		tmpfile = file + ".dl";
		
		DLUtils.makeEmptyFile(file);
		
		RandomAccessFile randomAccessFile = openRandomAccessFile();
		try {
			if(length > 0)
				randomAccessFile.setLength(length);
		} finally {
			DLUtils.close(randomAccessFile);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public String getFile() {
		return file;
	}
	
	public void setFile(String file) {
		this.file = file;
	}
	
	public String getTmpfile() {
		return tmpfile;
	}
	
	public void setTmpfile(String tmpfile) {
		this.tmpfile = tmpfile;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public boolean isSupportedContentRange() {
		return isSupportedContentRange;
	}

	public void setSupportedContentRange(boolean isSupportedContentRange) {
		this.isSupportedContentRange = isSupportedContentRange;
	}

	public List<DLBlock> getBlocks() {
		return blocks;
	}
	
	public void setBlocks(List<DLBlock> blocks) {
		this.blocks = blocks;
	}
	
	public List<DLThread> getThreads() {
		return threads;
	}

	public void setThreads(List<DLThread> threads) {
		this.threads = threads;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
		DLCallback.trigger(onStatusChange, this);
	}

	public void setOnThreadsShutdown(DLCallback<DLTask> onThreadsShutdown) {
		this.onThreadsShutdown = onThreadsShutdown;
	}

	public RandomAccessFile openRandomAccessFile() throws FileNotFoundException {
		return new RandomAccessFile(tmpfile, "rwd");
	}

	public synchronized void checkDone() throws FileNotFoundException {
		if(isDone())
			return;

		boolean allIsDone = true;
		boolean allIsShutdown = true;
		
		for (DLBlock block : blocks) {
			if(block.isRunning()) {
				allIsShutdown = false;
			}
			if(!block.isDone()) {
				allIsDone = false;
				if(!allIsShutdown)
					break;
			}
		}
		
		if(allIsDone) {
			setStatus(STATUS_DONE);
			
			new File(file).delete();
			new File(tmpfile).renameTo(new File(file));
		}
		
		if(allIsShutdown && onThreadsShutdown != null) {
			DLCallback.trigger(onThreadsShutdown, this);
		}
	}

	public boolean delete() {
		System.out.println("threads: "+Thread.currentThread().getThreadGroup().activeCount());
		System.out.println("delete file");
		return new File(file).delete()
				&& new File(file+".dl").delete();
	}

	@JSONField(serialize=false)
	public boolean isPause() {
		return status == STATUS_PAUSE;
	}

	@JSONField(serialize=false)
	public boolean isRunning() {
		return status == STATUS_RUNNING;
	}

	@JSONField(serialize=false)
	public boolean isDone() {
		return status == STATUS_DONE;
	}

	@JSONField(serialize=false)
	public boolean isWaiting() {
		return status == STATUS_WATING;
	}

	@JSONField(serialize=false)
	public int getTaskId() {
		return taskId;
	}
	
	@Override
	public String toString() {
		String xuchuan = isSupportedContentRange?"支持续传":"不支持续传";
		int threadCount = (threads == null)?0:threads.size();
		int blockCount = (blocks == null)?0:blocks.size();
		long downloaded = 0;
		if(blocks!=null) {
			synchronized (this) {
				for (DLBlock block : blocks) {
					downloaded+=block.getDownloaded();
				}
			}
		}
		String progress = DLUtils.getLengthStr(downloaded)+"/"+DLUtils.getLengthStr(length) + " " + DLUtils.getPercentStr(downloaded, length);
		return taskId + ": " + progress + "\t|" + DLUtils.getLengthStr(getSpeed()) + "/s \t|限制" + DLUtils.getLengthStr(limit)+ "|"+ threadCount+"线程|"+blockCount+"块|" + xuchuan + "|" + STATUS_STR[status] + "|"+name;
	}

	public long getSpeed() {
		long speed = 0;
		synchronized (this) {
			if(blocks != null) {
				for (DLBlock block : blocks) {
					speed += block.getSpeedMonitor().getSpeed();
				}
			}
		}
		return speed;
	}

	public void setSaveDir(String saveDir) {
		this.saveDir = saveDir;
	}
	
	public String getSaveDir() {
		return saveDir;
	}
	
	public void changeSaveDir(String saveDir) throws IOException {
		if(saveDir.equals(this.saveDir)) {
			return;
		}
		
		if(tmpfile != null) {
			String newFile = DLUtils.checkFileAndRename(saveDir + "/" + name);
			String newTmpfile = newFile + ".dl";
			
			if(new File(file).renameTo(new File(newFile))) {
				this.file = newFile;
				
				if(new File(newTmpfile).renameTo(new File(newTmpfile))) {
					this.tmpfile = newTmpfile;
				} else {
					new File(newFile).renameTo(new File(file));
				}
			} else {
				throw new IOException("fail move " + file + "->" + newFile + " and " + tmpfile + "->" + newTmpfile);
			}
		} else {
			file = saveDir + "/" + name;
		}
	}
	
	public void setLimit(int bytes) {
		this.limit = bytes;
		if(blocks != null) {
			synchronized (this) {
				int blockLimit = bytes / getThreads().size();
				for (DLBlock block : blocks) {
					block.getSpeedMonitor().setLimit(blockLimit);
				}
			}
		}
	}
}
