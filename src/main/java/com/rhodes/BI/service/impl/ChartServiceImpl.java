package com.rhodes.BI.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rhodes.BI.common.ErrorCode;
import com.rhodes.BI.constant.CommonConstant;
import com.rhodes.BI.exception.BusinessException;
import com.rhodes.BI.exception.ThrowUtils;
import com.rhodes.BI.manager.AiManager;
import com.rhodes.BI.mapper.ChartMapper;
import com.rhodes.BI.model.dto.chart.ChartQueryRequest;
import com.rhodes.BI.model.dto.chart.GenChartByAiRequest;
import com.rhodes.BI.model.entity.Chart;
import com.rhodes.BI.model.entity.User;
import com.rhodes.BI.model.vo.BiVO;
import com.rhodes.BI.service.ChartService;
import com.rhodes.BI.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author BPAA03
 * @description 针对表【chart(图标信息表)】的数据库操作Service实现
 * @createDate 2024-05-08 18:42:11
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {


    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        Long uid = chartQueryRequest.getUid();
        String goal = chartQueryRequest.getGoal();
        String chartName = chartQueryRequest.getChartName();
        String chartType = chartQueryRequest.getChartType();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(ObjectUtils.isNotEmpty(id) && id > 0, "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(uid), "uid", uid);
        queryWrapper.eq(StringUtils.isNotEmpty(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotEmpty(chartName), "chart_name", chartName);
        queryWrapper.eq(StringUtils.isNotEmpty(chartType), "chart_type", chartType);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}




