package com.SeimiCrawlerDemo.model;

import java.util.Date;

public class CsdnBlog {

	private long id;

	private long blogId;

	private String userId;

	private String title;

	private Date createTime;

	private int viewCount;

	private String content;
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getBlogId() {
		return blogId;
	}

	public void setBlogId(long blogId) {
		this.blogId = blogId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public int getViewCount() {
		return viewCount;
	}

	public void setViewCount(int viewCount) {
		this.viewCount = viewCount;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public String toString() {
		return "CsdnBlog [id=" + id + ", blogId=" + blogId + ", userId=" + userId + ", title=" + title + ", createTime="
				+ createTime + ", viewCount=" + viewCount + ", content=" + content + "]";
	}

}
