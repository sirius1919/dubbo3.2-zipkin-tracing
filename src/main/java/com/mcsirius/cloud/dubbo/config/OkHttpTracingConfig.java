package com.mcsirius.cloud.dubbo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: bakerZhu
 * @description:
 * @time: 2018年09月15日
 * @modifytime:
 */
public class OkHttpTracingConfig extends AbstractConfig {

	private static final Logger logger = LoggerFactory.getLogger(OkHttpTracingConfig.class);

	public OkHttpTracingConfig(String SERVICE_NAME, String ZIPKIN_V2_URL) {
		super(SERVICE_NAME, ZIPKIN_V2_URL);
	}

	@Override
	public Sender getSender(String zipkinV2Url) {
		return OkHttpSender.create(zipkinV2Url);
	}

}