package com.rhodes.BI.mq_example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

/**
 * Direct 交换机示例代码(消费者)
 */
public class DirectConsumer {

    private static final String EXCHANGE_NAME = "direct-exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("admin");
        factory.setPassword("123456");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "direct");

        String queueName1 = "xiaoyu_queue";
        channel.queueDeclare(queueName1, true, false, false, null);
        channel.queueBind(queueName1, EXCHANGE_NAME, "xiaoyu");

        String queueName2 = "xiaopi_queue";
        channel.queueDeclare(queueName2, true, false, false, null);
        channel.queueBind(queueName2 , EXCHANGE_NAME, "xiaopi");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback xiaoyuDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [xiaoyu] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        DeliverCallback xiaopiDeliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [xiaopi] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        channel.basicConsume(queueName1, true, xiaoyuDeliverCallback, consumerTag -> {
        });
        channel.basicConsume(queueName2, true, xiaopiDeliverCallback, consumerTag -> {
        });
    }
}