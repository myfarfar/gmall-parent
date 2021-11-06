package com.atguigu.gmall.list.receiver;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author atguigu-mqx
 */
@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    //  实现商品上架：
    //  监听消息！
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void goodsUpper(Long skuId, Message message, Channel channel){
        //  判断
        try {
            if (skuId!=null){
                //  int i = 1/0;
                searchService.upperGoods(skuId);
            }
        } catch (Exception e) {
            //  第三个参数是否重回队列！
            //  channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,false);
            //  记录日志表： skuId 上架没有成功！ insert into tname value(skuId,...);
            //  发短信提示......
            e.printStackTrace();
        }
        //  手动确认！
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  商品下架
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void goodsLower(Long skuId, Message message, Channel channel){
        //  判断
        try {
            if (skuId!=null){
                //  int i = 1/0;
                searchService.lowerGoods(skuId);
            }
        } catch (Exception e) {
            //  第三个参数是否重回队列！
            //  channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,false);
            //  记录日志表： skuId 上架没有成功！ insert into tname value(skuId,...);
            //  发短信提示......
            e.printStackTrace();
        }
        //  手动确认！
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}