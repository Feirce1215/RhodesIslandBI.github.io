package com.rhodes.BI.mq_example;

import com.rabbitmq.client.*;

/**
 * 多消费者队列示例代码（消费者）
 */
public class MultiConsumer {

    private static final String TASK_QUEUE_NAME = "multi_queue";

    public static void main(String[] argv) throws Exception {
        // 建立连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("123456");
        // 建立连接，创建频道
        final Connection connection = factory.newConnection();
        for (int i = 1; i < 3; i++) {
            final Channel channel = connection.createChannel();
            // 创建队列
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

            // 控制单个消费者的处理任务积压数
            channel.basicQos(1);

            int finalI = i;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                try {
                    try {
                        // 处理消息
                        System.out.println(" [x] Received '" + "编号：" + finalI + ":" + message + "'");
                        Thread.sleep(10000);
                        // 指定确认某条消息
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
                    } catch (InterruptedException _ignored) {
                        Thread.currentThread().interrupt();
                        // 指定拒绝某条消息
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
                    }
                } finally {
                    System.out.println(" [x] Done");
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
            };
            // 开启消费监听
            channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> {});
        }
    }
}