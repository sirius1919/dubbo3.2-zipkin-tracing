package com.mcsirius.cloud.dubbo;

import brave.Tracing;
import com.mcsirius.cloud.dubbo.config.AbstractConfig;
import com.mcsirius.cloud.dubbo.config.KafkaTracingConfig;
import com.mcsirius.cloud.dubbo.config.OkHttpTracingConfig;
import com.mcsirius.cloud.dubbo.enums.TransportEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: mcsirius.wang
 * @description:
 * @time: 2024年07月05日
 * @modifytime:
 */
public class SingletonTracingFactory {

	private static final Logger logger = LoggerFactory.getLogger(SingletonTracingFactory.class);

	private static AbstractConfig config;
	private static Tracing tracing;

	/**
	 * 构建tracing上下文
	 */
	private static void createInstance() {
		if(TransportEnum.KAFKA.getType().equals(TracingMetaInfo.TRANSPORT_TYPE)) {
			config = new KafkaTracingConfig(TracingMetaInfo.SERVICE_NAME, TracingMetaInfo.ZIPKIN_V2_URL, TracingMetaInfo.KAFKA_TOPIC);
		} else {
			config = new OkHttpTracingConfig(TracingMetaInfo.SERVICE_NAME, TracingMetaInfo.ZIPKIN_V2_URL);
		}
		tracing = config.tracing();
		logger.info("create tracing successful . SERVICE_NAME : " + TracingMetaInfo.SERVICE_NAME + ".  ZIPKIN_V2_URL : " + TracingMetaInfo.ZIPKIN_V2_URL);
	}

	public static Tracing getTracing() {
		if(tracing == null) {
			synchronized (SingletonTracingFactory.class) {
				if(tracing == null) {
					createInstance();
				}
			}
		}
		return tracing;
	}
}
