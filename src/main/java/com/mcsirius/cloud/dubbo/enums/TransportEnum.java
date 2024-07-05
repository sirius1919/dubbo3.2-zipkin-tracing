package com.mcsirius.cloud.dubbo.enums;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: mcsirius.wang
 * @description: 数据传输方式
 * @time: 2024年07月05日
 * @modifytime:
 */
public enum  TransportEnum {
	HTTP("http"),
	KAFKA("kafka");

	private String type;

	TransportEnum(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
