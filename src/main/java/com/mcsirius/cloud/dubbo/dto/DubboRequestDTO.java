package com.mcsirius.cloud.dubbo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: mcsirius.wang
 * @description: dubbo请求体
 * @time: 2024年07月05日
 * @modifytime:
 */
@Data
public class DubboRequestDTO implements Serializable {
    private String interfaceClass;
    private String methodName;
    private Object[] args;
}
