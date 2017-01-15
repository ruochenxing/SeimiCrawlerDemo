/*
   Copyright 2015 Wang Haomiao<et.tw@163.com>

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package cn.wanghaomiao.seimi.struct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;

import com.SeimiCrawlerDemo.ProgressBarThread;

import cn.wanghaomiao.seimi.core.SeimiBeanResolver;
import cn.wanghaomiao.seimi.exception.SeimiProcessExcepiton;
import cn.wanghaomiao.seimi.http.SeimiHttpType;
import cn.wanghaomiao.xpath.model.JXDocument;

/**
 * 抓取请求的返回结果
 *
 * @author 汪浩淼 [et.tw@163.com] Date: 2015/05/12.
 */
public class Response extends CommonObject {
	private static final long serialVersionUID = 2230167844920698187L;
	
	private BodyType bodyType;
	private Request request;
	private String charset;
	private String referer;
	private byte[] data;
	private String content;
	/**
	 * 这个主要用于存储上游传递的一些自定义数据
	 */
	private Map<String, String> meta;
	private String url;
	private Map<String, String> params;
	/**
	 * 网页内容真实源地址
	 */
	private String realUrl;
	/**
	 * 此次请求结果的http处理器类型
	 */
	private SeimiHttpType seimiHttpType;

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public BodyType getBodyType() {
		return bodyType;
	}

	public void setBodyType(BodyType bodyType) {
		this.bodyType = bodyType;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Map<String, String> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, String> meta) {
		this.meta = meta;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	public String getRealUrl() {
		return realUrl;
	}

	public void setRealUrl(String realUrl) {
		this.realUrl = realUrl;
	}

	public SeimiHttpType getSeimiHttpType() {
		return seimiHttpType;
	}

	public void setSeimiHttpType(SeimiHttpType seimiHttpType) {
		this.seimiHttpType = seimiHttpType;
	}

	/**
	 * 通过bean中定义的Xpath注解进行自动填充
	 *
	 * @param bean
	 * @param <T>
	 * @return
	 * @throws Exception
	 */
	public <T> T render(Class<T> bean) throws Exception {
		if (bodyType.equals(BodyType.TEXT)) {
			return SeimiBeanResolver.parse(bean, this.content);
		} else {
			throw new SeimiProcessExcepiton("can not parse struct from binary");
		}
	}

	public JXDocument document() {
		return BodyType.TEXT.equals(bodyType) && content != null ? new JXDocument(content) : null;
	}

	public void saveTo(File targetFile) {

		try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
				FileChannel fo = fileOutputStream.getChannel();) {
			File pf = targetFile.getParentFile();
			if (!pf.exists()) {
				pf.mkdirs();
			}
			if (BodyType.TEXT.equals(bodyType)) {
				fo.write(ByteBuffer.wrap(getContent().getBytes()));
			} else {
				fo.write(ByteBuffer.wrap(getData()));
			}
		} catch (Exception e) {
			throw new SeimiProcessExcepiton(e);
		}
	}

	public void saveToInProgressBar(File targetFile) {
		System.out.println("saveToInProgressBar");
		if (!BodyType.TEXT.equals(bodyType)) {
			try {
				URL url=new URL(getUrl());
				InputStream fis = url.openStream();
				int length = fis.available();
				System.out.println(length);
				ProgressBarThread pbt = new ProgressBarThread(length);// 创建进度条
				new Thread(pbt).start();// 开启线程，刷新进度条
				FileOutputStream fos = new FileOutputStream(targetFile);
				byte[] buf = new byte[1024];
				int size = 0;
				while ((size = fis.read(buf)) > -1) {// 循环读取
					fos.write(buf, 0, size);
					pbt.updateProgress(size);// 写完一次，更新进度条
				}
				pbt.finish();// 文件读取完成，关闭进度条
				fos.flush();
				fos.close();
				fis.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
