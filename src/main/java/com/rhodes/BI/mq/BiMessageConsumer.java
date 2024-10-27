package com.rhodes.BI.mq;

import com.rabbitmq.client.Channel;
import com.rhodes.BI.common.ErrorCode;
import com.rhodes.BI.exception.BusinessException;
import com.rhodes.BI.manager.AiManager;
import com.rhodes.BI.model.entity.Chart;
import com.rhodes.BI.service.ChartService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 消费者
 */
@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private AiManager aiManager;

    @Resource
    private ChartService chartService;

    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        if (StringUtils.isBlank(message)) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            channel.basicReject(deliveryTag, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }
        // 更新任务状态，AI任务执行前
        Chart updateStatus = new Chart();
        updateStatus.setId(chart.getId());
        updateStatus.setStatus("running");
        boolean res = chartService.updateById(updateStatus);
        if (!res) {
            channel.basicReject(deliveryTag, false);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "状态更新失败");
        }
        // 调用AI
        String result = aiManager.doChat(buildUserInput(chart));
        String[] splits = result.split("·····");
        if (splits.length < 3) {
            channel.basicReject(deliveryTag, false);
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
            channel.basicReject(deliveryTag, false);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "状态更新失败");
        }
        // 消息确认
        channel.basicAck(deliveryTag, false);
    }

    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getOriginData();

        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求:").append(goal).append("\n");
        if (StringUtils.isNotBlank(chartType)) {
            userInput.append("请使用").append(chartType).append("\n");
        }
        userInput.append("原始数据:").append("\n");
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }
}