package com.download;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Properties;

import com.download.io.BufferedRandomOutputStream;


/**
 * 下载线程，负责下载DLBlock
 */
public class DLThread implements Runnable {

	private static final int INPUT_BUFFER_SIZE = 1024;
	private static final int OUTPUT_BUFFER_SIZE = 8192;
	
	private DLTask task;//所属下载任务
	private DLBlock block;//下载块
	private boolean isDone;
	private boolean isStop;
	
	public DLBlock getBlock() {
		return block;
	}
	
	public DLThread(DLTask task, DLBlock block) {
		this.task = task;
		this.block = block;
	}

	public void run() {
		
		block.setStatus(DLBlock.STATUS_RUNNING);
		
		BufferedRandomOutputStream randomOutputStream = null;
		try {
			
			randomOutputStream = new BufferedRandomOutputStream(task.openRandomAccessFile(), OUTPUT_BUFFER_SIZE);
			
		} catch (FileNotFoundException e) {
			block.setThrowable(e);
			e.printStackTrace();
			isDone = true;
			return;
		}

		HttpURLConnection conn = null;
		long downloadPosition = 0;
		try {
			
			if(task.isSupportedContentRange()) {//断点续传
				block.setDownloaded(Math.max(block.getDownloaded() - OUTPUT_BUFFER_SIZE, 0));// -8kb保证下载结果(有可能之前写入失败)
				
				downloadPosition = block.getBegin() + block.getDownloaded();
				
				randomOutputStream.seek(downloadPosition);
			}
			
			Properties reqProps = new Properties();
			if(task.isSupportedContentRange()) {//设置下载范围
				reqProps.setProperty("Range", "bytes=" + downloadPosition + "-" + block.getEnd());
			}
			
			conn = DLUtils.getChromeURLConnection(task.getUri(), false, reqProps);
			InputStream in = new BufferedInputStream(DLUtils.getInputStream(conn.getInputStream(), conn.getContentEncoding()), INPUT_BUFFER_SIZE);
			
			byte[] buf = new byte[INPUT_BUFFER_SIZE];
			int len;
			
			while((len = in.read(buf)) != -1) {
				
				randomOutputStream.write(buf, 0, len);

				block.updateDownloaded(len);
				
				downloadPosition += len;
				
				if(task.isSupportedContentRange() && downloadPosition >= block.getEnd()
						|| isStop
						|| !task.isRunning()) {
					//也许将任务部分分配给了其他线程（增加了线程），在后面做详细处理
					break;
				}
			}
			
		} catch (IOException e) {
			
			block.setThrowable(e);
			e.printStackTrace();
			
		} finally {

			DLUtils.close(conn);
			DLUtils.close(randomOutputStream);
			
			isDone = true;
			
			if(downloadPosition >= block.getEnd()) {
	
				//块下载完成
				block.setStatus(DLBlock.STATUS_DONE);
				
			} else {
				
				//停止下载
				block.setStatus(DLBlock.STATUS_PAUSE);
				
			}
			
			try {//检查并设置任务状态
				task.checkDone();
			} catch (FileNotFoundException e) {
				
				block.setThrowable(e);
				e.printStackTrace();
				
			}
		}
	}
	
	public boolean isDone() {
		return isDone;
	}

	public void stop() {
		isStop = true;
	}
}
