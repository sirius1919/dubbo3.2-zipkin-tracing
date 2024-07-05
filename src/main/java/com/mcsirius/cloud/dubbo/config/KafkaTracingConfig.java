package com.mcsirius.cloud.dubbo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.codec.Encoding;
import zipkin2.reporter.Sender;
import zipkin2.reporter.kafka.KafkaSender;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: mcsirius.wang
 * @description: kafka tracing 配置类
 * @time: 2024年07月05日
 * @modifytime:
 */
public class KafkaTracingConfig extends AbstractConfig{

	private static final Logger logger = LoggerFactory.getLogger(KafkaTracingConfig.class);

	private String KAFKA_TOPIC ;

	public KafkaTracingConfig(String SERVICE_NAME, String ZIPKIN_V2_URL, String KAFKA_TOPIC) {
		super(SERVICE_NAME, ZIPKIN_V2_URL);
		this.KAFKA_TOPIC = KAFKA_TOPIC;
	}

	@Override
	public Sender getSender(String zipkinV2Url) {
		KafkaSender sender = KafkaSender.newBuilder()
				.bootstrapServers(zipkinV2Url)
				.topic(KAFKA_TOPIC)
				.encoding(Encoding.JSON)
				.build();
		return sender;
	}


}