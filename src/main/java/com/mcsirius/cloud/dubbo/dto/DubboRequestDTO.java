package com.mcsirius.cloud.dubbo.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DubboRequestDTO implements Serializable {
    private String interfaceClass;
    private String methodName;
    private Object[] args;
}
