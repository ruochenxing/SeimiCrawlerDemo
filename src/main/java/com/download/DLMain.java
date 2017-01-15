package com.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * 主程序
 */
public class DLMain {

	private transient AtomicLong cfgSaveTime = new AtomicLong();//配置文件保存时间
	private transient List<DLTask> runningTasks = new ArrayList<DLTask>();//正在下载的任务（非等待状态）
	private List<DLTask> tasks = new ArrayList<DLTask>();//所有任务
	private int threadCount = 2;
	private int taskCount = 5;
	private int limit = 2048*1024;//bytes
	private String cfgDir = "./";
	private String saveDir = "./";
	private final DLCallback<DLBlock> onBlockStatusChange;
	private final DLCallback<DLBlock> onBlockDownloadChange;
	private final DLCallback<DLTask> onTaskStatusChange;
	
	public DLMain() throws IOException {
		
		onBlockStatusChange = new DLCallback<DLBlock>() {
			@Override
			public void callback(DLBlock arg) {
				DLMain.this.onBlockStatusChange(arg.getTask(), arg);
			}
		};
		
		onBlockDownloadChange = new DLCallback<DLBlock>() {
			@Override
			public void callback(DLBlock arg) {
				DLMain.this.onBlockDownloadChange(arg.getTask(), arg);				
			}
		};
		
		onTaskStatusChange = new DLCallback<DLTask>() {
			
			@Override
			public void callback(DLTask arg) {
				DLMain.this.onTaskStatusChange(arg);
			}
		};
		
		loadConfigs(cfgDir);
	}
	
	public int getLimit() {
		return limit;
	}
	
	public void setLimit(int bytes) {
		this.limit = bytes;
		onLimitChange(bytes);
	}
	

	public List<DLTask> getRunningTasks() {
		return runningTasks;
	}
	
	public void setRunningTasks(List<DLTask> runningTasks) {
		this.runningTasks = runningTasks;
	}
	
	public int getTaskCount() {
		return taskCount;
	}
	
	public void setTaskCount(int taskCount) {
		this.taskCount = taskCount;
		onTaskCountChange(taskCount);
	}

	public int getThreadCount() {
		return threadCount;
	}
	
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
		onThreadCountChange(threadCount);
	}

	public List<DLTask> getTasks() {
		return tasks;
	}
	
	public void setTasks(List<DLTask> tasks) {
		this.tasks = tasks;
		onTasksChange(tasks);
	}
	
	public void setSaveDir(String saveDir) {
		this.saveDir = saveDir;
		onSaveDirChange(saveDir);
	}
	
	public String getSaveDir() {
		return saveDir;
	}
	
	public DLTask findTaskById(int id) {
		for (DLTask task : getTasks()) {
			if(task.getTaskId() == id) {
				return task;
			}
		}
		return null;
	}
	
	public void loadConfigs(String dir) throws FileNotFoundException, IOException {
		this.cfgDir = dir;
		File dlCfg = new File(dir + "dl.config");
		if(dlCfg.exists()) {
			String content = DLUtils.readFull(new FileInputStream(dlCfg), "utf-8");
			JSONObject config = JSON.parseObject(content);
			
			if(config == null) {
				return;
			}

			List<DLTask> loadTasks = JSON.parseObject(config.getString("tasks"), new TypeReference<List<DLTask>>(){});
			
			for (DLTask task : loadTasks) {
				if(task.isRunning()) {
					task.setStatus(DLTask.STATUS_PAUSE);
				}
				if(task.getBlocks()!=null) {
					for (DLBlock block : task.getBlocks()) {
						if(block.isRunning())
							block.setStatus(DLBlock.STATUS_PAUSE);
						block.setTask(task);
						block.setOnDownloadChange(onBlockDownloadChange);
						block.setOnStatusChange(onBlockStatusChange);
					}
				}
				task.setOnStatusChange(onTaskStatusChange);
			}

			this.tasks.addAll(loadTasks);
			
			
			this.onTasksChange(this.tasks);
			
			this.setSaveDir(config.getString("saveDir"));
			this.setThreadCount(config.getIntValue("threadCount"));
			this.setTaskCount(config.getIntValue("taskCount"));
		}
	}
	
	public void saveConfigs() {
		FileWriter fw = null;
		try {
			fw = new FileWriter(cfgDir + "/dl.config");
			JSON.writeJSONStringTo(this, fw, SerializerFeature.PrettyFormat);
			cfgSaveTime.set(System.currentTimeMillis());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			DLUtils.close(fw);
		}
	}
	
	/**
	 * 检查 并更新正在下载的任务数
	 */
	public void checkRunningTasks() {
		synchronized (runningTasks) {//runningTasks 会被多线程使用，见 onTaskStatusChange
			if(runningTasks.size() > taskCount) {
				
				List<DLTask> removeTasks = new ArrayList<DLTask>();
				for (int i = runningTasks.size()-1; 0 <= i && i > taskCount; i--) {
					
					DLTask task = runningTasks.get(i);
					task.setStatus(DLTask.STATUS_WATING);
					
					removeTasks.add(task);
				}
				
			} else if(runningTasks.size() < taskCount) {
				
				List<DLTask> addTasks = new ArrayList<DLTask>();
				
				for (DLTask task : tasks) {
					if(task.isWaiting()) {
						
						startTask(task);
						addTasks.add(task);
						
						if(addTasks.size() + runningTasks.size() >= taskCount) {
							break;
						}
					}
				}
			}
		}
	}

	protected void onLimitChange(int bytes) {
		for (DLTask task : runningTasks) {
			task.setLimit(bytes);
		}
		saveConfigs();
	}

	protected void onSaveDirChange(String saveDir) {
		new File(saveDir).mkdirs();
		
		for (DLTask task : tasks) {
			if(!task.isRunning() && !task.isWaiting()) {
				try {
					task.changeSaveDir(saveDir);
				} catch (IOException e) {//忽略?
					e.printStackTrace();
				}
			}
		}
		saveConfigs();
	}

	protected void onTaskCountChange(int taskCount) {
		saveConfigs();
		checkRunningTasks();
	}

	protected void onThreadCountChange(int threadCount) {
		saveConfigs();
		for (DLTask task : tasks) {
			if(task.isRunning()) {
				this.checkTaskThreads(task);
			}
		}
	}

	protected void onTasksChange(List<DLTask> tasks) {
		saveConfigs();
	}

	/**
	 * 某任务的状态更新(多线程调用)
	 * @param task
	 */
	protected void onTaskStatusChange(DLTask task) {
		if(task.isRunning()) {
			synchronized (runningTasks) {
				if(!runningTasks.contains(task))
					runningTasks.add(task);
			}
			task.setLimit(limit);
		} else {
			synchronized (runningTasks) {
				runningTasks.remove(task);
			}
		}
		saveConfigs();
	}

	
	/**
	 * 某任务的块的状态更新(多线程调用)
	 * @param task
	 * @param status
	 */
	protected void onBlockStatusChange(DLTask task, DLBlock block) {
		if((block.isDone() || block.isPause()) && task.isRunning()) {
			checkTaskThreads(task);
		}
		saveConfigs();
	}

	/**
	 * 下载进度更新(多线程调用)
	 * @param task
	 * @param block
	 */
	protected void onBlockDownloadChange(DLTask task, DLBlock block) {
		synchronized (cfgSaveTime) {
			if(cfgSaveTime.get() + 2000 < System.currentTimeMillis()) {
				cfgSaveTime.set(System.currentTimeMillis());
				saveConfigs();
			}
		}
	}

	/**
	 * 检查、更新下载线程数(多线程调用)
	 * @param task
	 */
	public void checkTaskThreads(DLTask task) {
		synchronized (task) {
			//移除已完成的线程
			for (Iterator<DLThread> it = task.getThreads().iterator(); it.hasNext();) {
				if(it.next().isDone())
					it.remove();
			}
			
			if(task.getThreads().size() < threadCount && task.isSupportedContentRange()) {// 新增线程
				for (DLBlock block : task.getBlocks()) {
					if(block.isPause()) {
						
						if(task.getThreads().size() >= threadCount) {
							break;
						}
						
						DLThread thread = new DLThread(task, block);
						task.getThreads().add(thread);
						
						new Thread(thread).start();
					}
				}
				
				if(task.getThreads().size() < threadCount) {//如果还有线程没有分配，则 拆分块
					
					//将下载得少的块放到前面
					List<DLBlock> blocks = new ArrayList<DLBlock>(task.getBlocks());
					Collections.sort(blocks, new Comparator<DLBlock>() {
						public int compare(DLBlock a, DLBlock b) {
							return (int) ((b.getEnd()-b.getBegin()-b.getDownloaded()) - (a.getEnd()-a.getBegin()-a.getDownloaded()));
						}
					});

					List<DLBlock> newBlocks = new ArrayList<DLBlock>();
					for (DLBlock block : blocks) {
						long begin = block.getBegin() + block.getDownloaded();
						long end = block.getEnd();
						
						long remaining = end - begin;
						if(remaining < 5120)
							break;
						
						block.setEnd(begin + remaining/2);

						//下载新拆分的块
						DLBlock newBlock = new DLBlock(task, onBlockDownloadChange, onBlockDownloadChange, begin + remaining/2, end);
						DLThread thread = new DLThread(task, newBlock);
						newBlocks.add(newBlock);
						task.getThreads().add(thread);
						
						new Thread(thread).start();
						
						if(task.getThreads().size() >= threadCount) {
							break;
						}
					}
					
					task.getBlocks().addAll(newBlocks);
				}
				
			} else {// 减少线程

				int kills = 0;
				for (DLThread thread : task.getThreads()) {
					
					if(task.getThreads().size() - kills <= threadCount) {
						break;
					}
					
					thread.stop();
					kills++;
				}
			}
		}
	}

	public synchronized boolean hasTask(String uri) {
		for (DLTask task : tasks) {
			if(uri.equals(task.getUri()))
				return true;
		}
		return false;
	}
	
	public void removeTask(DLTask task, boolean deleleFile) {
		if(deleleFile) {
			task.delete();
			// if running
			task.setOnThreadsShutdown(new DLCallback<DLTask>() {
				@Override
				public void callback(DLTask task) {
						task.delete();
					task.setOnThreadsShutdown(null);
				}
			});
		}
		task.setStatus(DLTask.STATUS_DEL);
		tasks.remove(task);
		onTasksChange(tasks);
	}
	/**
	 * 根据uri生成下载任务
	 * */
	public DLTask getTaskUriInfo(String uri,String dir) throws MalformedURLException, IOException {
		Properties reqProps = new Properties();
		reqProps.setProperty("Range", "bytes=0-");
		
		HttpURLConnection conn = null;
		try {
			conn = DLUtils.getChromeURLConnection(uri, false, reqProps);
			conn.connect();
			
			boolean supportedContentRange = (conn.getHeaderField("Content-Range") != null);//是否支持断点续传
			int length = conn.getContentLength();//获取资源长度
			String name = DLUtils.getNameForDisposition(conn.getHeaderField("Content-Disposition"));
			if(name == null) {
				name = uri.substring(Math.max(uri.lastIndexOf('/'), uri.lastIndexOf('\\'))+1);
			}
			
			if(name.isEmpty()) {
				name = "未知文件";
			}
			name = name.replaceAll("[:*?<>|]", "_");//去除无效字符
			
			return new DLTask(onTaskStatusChange, uri, name, dir, length, supportedContentRange);
		} finally {
			DLUtils.close(conn);
		}
	}

	public synchronized boolean addTask(DLTask task) throws FileNotFoundException, IOException {
		tasks.add(task);
		task.makeFile();
		saveConfigs();
		return false;
	}

	public void startTask(DLTask task) {
		
		if(this.runningTasks.size() >= taskCount) {
			task.setStatus(DLTask.STATUS_WATING);
			return;
		}
		
		List<DLBlock> blocks = task.getBlocks();
		if(blocks == null) {
			blocks = makeBlocks(task, task.isSupportedContentRange()?threadCount:1);
		}
		
		List<DLThread> threads = new ArrayList<DLThread>(blocks.size());
		for (DLBlock block : blocks) {
			if(block.isPause()) {
				DLThread thread = new DLThread(task, block);
				threads.add(thread);
				new Thread(thread).start();
				if(threads.size()>= threadCount) {
					break;
				}
			}
		}
		
		task.setThreads(threads);

		if(!task.getThreads().isEmpty())
			task.setStatus(DLTask.STATUS_RUNNING);
	}
	

	/**
	 * 设置分块下载的块大小
	 * @param threadCount 线程数
	 * @return
	 */
	public List<DLBlock> makeBlocks(DLTask task, int threadCount) {
		int blockSize = (int) Math.floor(task.getLength()/ threadCount);
		ArrayList<DLBlock> blocks = new ArrayList<DLBlock>(threadCount);
		for (int i = 0; i < threadCount; i++) {
			long begin = i*blockSize;
			long end = begin+blockSize;
			blocks.add(new DLBlock(task, onBlockStatusChange, onBlockDownloadChange, begin, end));
		}
		blocks.get(threadCount-1).setEnd(task.getLength());
		task.setBlocks(blocks);
		return blocks;
	}

	/**
	 * 开始所有任务
	 */
	public void startAll() {
		for (DLTask task : tasks) {
			if(task.isPause())
				task.setStatus(DLTask.STATUS_WATING);
		}
		checkRunningTasks();
	}
}
