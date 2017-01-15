package com.SeimiCrawlerDemo.dao;

import com.SeimiCrawlerDemo.model.CsdnBlog;
import com.SeimiCrawlerDemo.model.DoubanMovie;

import net.paoding.rose.jade.annotation.DAO;
import net.paoding.rose.jade.annotation.ReturnGeneratedKeys;
import net.paoding.rose.jade.annotation.SQL;

@DAO
public interface StoreToDbDAO {
	@ReturnGeneratedKeys
	@SQL("insert into movie(title,rateResult,rateNum) values(:1.title,:1.rateResult,:1.rateNum)")
	public int save(DoubanMovie movie);

	@ReturnGeneratedKeys
	@SQL("insert ignore into blog(blogId,title,content,createTime,viewCount,userId) values(:1.blogId,:1.title,:1.content,:1.createTime,:1.viewCount,:1.userId)")
	public int save(CsdnBlog blog);
}
