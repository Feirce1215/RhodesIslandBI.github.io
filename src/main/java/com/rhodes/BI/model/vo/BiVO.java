package com.rhodes.BI.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class BiVO implements Serializable {

    /**
     * 图表id
     */
    private Long id;

    /**
     * 生成的图标代码
     */
    private String genChart;

    /**
     * 生成的分析结果
     */
    private String genResult;
}
