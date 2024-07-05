package com.mcsirius.cloud.dubbo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: mcsirius.wang
 * @description: dubbo返回体
 * @time: 2024年07月05日
 * @modifytime:
 */
@Data
public class DubboResponseDTO implements Serializable {
    private String interfaceClassName;
    private String methodName;
    private String result;
    private long spendTime;
}
