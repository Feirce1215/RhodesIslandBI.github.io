package com.rhodes.BI.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rhodes.BI.model.dto.chart.ChartQueryRequest;
import com.rhodes.BI.model.dto.chart.GenChartByAiRequest;
import com.rhodes.BI.model.entity.Chart;
import com.rhodes.BI.model.entity.User;
import com.rhodes.BI.model.vo.BiVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author BPAA03
* @description 针对表【chart(图标信息表)】的数据库操作Service
* @createDate 2024-05-08 18:42:11
*/
public interface ChartService extends IService<Chart> {

//    /**
//     * 通过AI生成图表和分析
//     *
//     * @param genChartByAiRequest
//     * @return
//     */
//    BiVO genChartByAi(GenChartByAiRequest genChartByAiRequest, String csvData, User loginUser);

    /**
     * 获取查询条件
     *
     * @param chartQueryRequest
     * @return
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

}
