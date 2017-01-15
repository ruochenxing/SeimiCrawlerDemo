package com.SeimiCrawlerDemo.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.SeimiCrawlerDemo.annotations.DoLog;
import cn.wanghaomiao.seimi.annotation.Interceptor;
import cn.wanghaomiao.seimi.core.SeimiInterceptor;
import cn.wanghaomiao.seimi.struct.Response;

//等同于默认的这个@Interceptor()
@Interceptor(everyMethod = false)
public class DemoInterceptor implements SeimiInterceptor {
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public Class<? extends Annotation> getTargetAnnotationClass() {
		return DoLog.class;
	}

	@Override
	public int getWeight() {
		return 8;
	}

	@Override
	public void before(Method method, Response response) {
		logger.info("在这里我可以对这个reponse做些操作的，reponseHashCode = {}", response.hashCode());

	}

	@Override
	public void after(Method method, Response response) {
		logger.info("method={}.{} has done!", method.getDeclaringClass().getSimpleName(), method.getName());
	}
}
