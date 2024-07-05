package com.mcsirius.cloud.dubbo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DubboResponseDTO implements Serializable {
    private String interfaceClassName;
    private String methodName;
    private String result;
    private long spendTime;
}
