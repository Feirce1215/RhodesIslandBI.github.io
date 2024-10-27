package com.rhodes.BI.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rhodes.BI.annotation.AuthCheck;
import com.rhodes.BI.common.BaseResponse;
import com.rhodes.BI.common.DeleteRequest;
import com.rhodes.BI.common.ErrorCode;
import com.rhodes.BI.common.ResultUtils;
import com.rhodes.BI.constant.UserConstant;
import com.rhodes.BI.exception.BusinessException;
import com.rhodes.BI.exception.ThrowUtils;
import com.rhodes.BI.manager.AiManager;
import com.rhodes.BI.manager.RedisLimiterManager;
import com.rhodes.BI.model.dto.chart.*;
import com.rhodes.BI.model.entity.Chart;
import com.rhodes.BI.model.entity.User;
import com.rhodes.BI.model.vo.BiVO;
import com.rhodes.BI.mq.BiMessageProducer;
import com.rhodes.BI.service.ChartService;
import com.rhodes.BI.service.UserService;
import com.rhodes.BI.utils.ExcelUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表接口
 *
 *
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUid(loginUser.getUid());

        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldchart = chartService.getById(id);
        ThrowUtils.throwIf(oldchart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldchart.getUid().equals(user.getUid()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldchart = chartService.getById(id);
        ThrowUtils.throwIf(oldchart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUid(loginUser.getUid());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);

//        List<String> tags = chartEditRequest.getTags();
//        if (tags != null) {
//            chart.setTags(JSONUtil.toJsonStr(tags));
//        }
//        // 参数校验
//        chartService.validchart(chart, false);

        User loginUser = userService.getLoginUser(request);

        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldchart = chartService.getById(id);
        ThrowUtils.throwIf(oldchart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldchart.getUid().equals(loginUser.getUid()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 智能分析（消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/mq")
    public BaseResponse<BiVO> genChartByAiAsyncMQ(@RequestPart("file") MultipartFile multipartFile,
                                                GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 1.大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        // 2.后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "存在非法的文件后缀");
        // 校验请求
        if (ObjectUtils.isEmpty(genChartByAiRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 限流判断
        User loginUser = userService.getLoginUser(request);
        redisLimiterManager.doRateLimiter("genChartByAi_" + loginUser.getUid());

        // 读取用户上传的 Excel 文件, 进行数据压缩
        String csvData = ExcelUtils.excelToCsv(multipartFile);

        String goal = genChartByAiRequest.getGoal();
        String chartName = genChartByAiRequest.getChartName();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        if (StringUtils.isAnyBlank(goal)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isAnyBlank(chartName) && chartName.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 插入到数据库
        Chart chart = new Chart();
        chart.setChartName(chartName);
        chart.setGoal(goal);
        chart.setOriginData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUid(loginUser.getUid());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 获取图表 id, 发送任务
        long chartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(chartId));

        // 返回给前端
        BiVO biResponse = new BiVO();
        biResponse.setId(chartId);

        return ResultUtils.success(biResponse);
    }


    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiVO> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 1.大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        // 2.后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "存在非法的文件后缀");
        // 校验请求
        if (ObjectUtils.isEmpty(genChartByAiRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 限流判断
        User loginUser = userService.getLoginUser(request);
        redisLimiterManager.doRateLimiter("genChartByAi_" + loginUser.getUid());

        // 读取用户上传的 Excel 文件, 进行数据压缩
        String csvData = ExcelUtils.excelToCsv(multipartFile);

        String goal = genChartByAiRequest.getGoal();
        String chartName = genChartByAiRequest.getChartName();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        if (StringUtils.isAnyBlank(goal)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isAnyBlank(chartName) && chartName.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append(goal).append("\n");
        if (StringUtils.isNotBlank(chartType)) {
            userInput.append("请使用").append(chartType).append("\n");
        }
        userInput.append("原始数据:").append("\n");
        userInput.append(csvData).append("\n");

        // 插入到数据库
        Chart chart = new Chart();
        chart.setChartName(chartName);
        chart.setGoal(goal);
        chart.setOriginData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUid(loginUser.getUid());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // todo 处理满队列异常
        CompletableFuture.runAsync(() -> {
            // 更新任务状态，AI任务执行前
            Chart updateStatus = new Chart();
            updateStatus.setId(chart.getId());
            updateStatus.setStatus("running");
            boolean res = chartService.updateById(updateStatus);
            if (!res) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "状态更新失败");
            }
            // 调用AI
            String result = aiManager.doChat(userInput.toString());
            String[] splits = result.split("·····");
            if (splits.length < 3) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }

            String genChart = splits[1].trim();
            String genResult = splits[2].trim();

            // 更新任务状态，AI任务执行后
            Chart updateResult = new Chart();
            updateResult.setId(chart.getId());
            updateResult.setStatus("succeed");
            updateResult.setGenChart(genChart);
            updateResult.setGenResult(genResult);
            res = chartService.updateById(updateResult);
            if (!res) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "状态更新失败");
            }
        }, threadPoolExecutor);

        // 返回给前端
        BiVO biResponse = new BiVO();
        biResponse.setId(chart.getId());

        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<BiVO> genChartByAI(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        // 校验请求
        if (ObjectUtils.isEmpty(genChartByAiRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 1.大小
        final long ONE_MB = 1024 * 1024L;
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        // 2.后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "存在非法的文件后缀");

        // 读取用户上传的 Excel 文件, 进行数据压缩
        String csvData = ExcelUtils.excelToCsv(multipartFile);

        User loginUser = userService.getLoginUser(request);

        String goal = genChartByAiRequest.getGoal();
        String chartName = genChartByAiRequest.getChartName();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        if (StringUtils.isAnyBlank(goal)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isAnyBlank(chartName) && chartName.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append(goal).append("\n");
        if (StringUtils.isNotBlank(chartType)) {
            userInput.append("请使用").append(chartType).append("\n");
        }
        userInput.append("原始数据:").append("\n");
        userInput.append(csvData).append("\n");

        String result = aiManager.doChat(userInput.toString());

        String[] splits = result.split("·····");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        // 插入到数据库
        Chart chart = new Chart();
        chart.setChartName(chartName);
        chart.setGoal(goal);
        chart.setOriginData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUid(loginUser.getUid());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        BiVO biResponse = new BiVO();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setId(chart.getId());
        return ResultUtils.success(biResponse);
    }
}
