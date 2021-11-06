package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    //  监听消息：取消订单！
    //  第一种：通过绑定设置 ，第二种：通过队列设置 基于插件的延迟消息队列时，通过配置类！
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel){
        //  orderInfo paymentInfo Alipay ！
        try {
            //  判断
            if (orderId!=null){
                //  通过 orderId 来获取到orderInfo 对象， 然后根据这个orderInfo 对象判断支付状态！
                OrderInfo orderInfo = orderService.getById(orderId);
                //  判断
                if (orderInfo!=null && "UNPAID".equals(orderInfo.getOrderStatus())
                        && "UNPAID".equals(orderInfo.getProcessStatus())){
                    //  此时关闭过期订单！ 肯定有orderInfo！
                    //  需要调用远程方法看是否有本地交易记录产生 ： 什么时候才会本地记录? 有二维码生产的时候！
                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    //  判断paymentInfo
                    if (paymentInfo!=null && "UNPAID".equals(paymentInfo.getPaymentStatus())){
                        //  说明既有orderInfo ，也有paymentInfo 记录！
                        //  查看是否有记录，如果有交易记录，才有可能关闭Alipay！ 要是没有交易记录，不用关闭Alipay！
                        Boolean flag = this.paymentFeignClient.checkPayment(orderId);
                        //  判断： flag = true 存在交易记录 用户扫描了二维码！
                        if (flag){
                            //  调用关闭支付宝交易记录方法！
                            Boolean result = this.paymentFeignClient.closePay(orderId);
                            //  判断 result = true ; 关闭成功！ 用户未付款！
                            if (result){
                                //  再次调用关闭orderInfo，paymentInfo！
                                orderService.execExpiredOrder(orderId,"2");
                            }else {
                                //  result = false ; 关闭失败！ 用户付款了！
                                //  this.paymentService.paySuccess(outTradeNo, PaymentType.ALIPAY.name(),paramMap);
                            }
                        }else {
                            //  flag = false 不存在交易记录 用户没有扫描二维码！ 关闭orderInfo 还有paymentInfo
                            //  只有orderInfo；
                            orderService.execExpiredOrder(orderId,"2");
                        }
                    }else {
                        //  只有orderInfo；
                        orderService.execExpiredOrder(orderId,"1");
                    }
                }
            }
        } catch (Exception e) {
            //  有异常，需要记录日志...
            e.printStackTrace();
        }

        //  手动确认消息！
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
    //  监听支付成功时发送的消息！
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void paymentPay(Long orderId, Message message,Channel channel){
        try {
            //  判断
            if(orderId!=null){
                //  查询订单对象
                OrderInfo orderInfo = this.orderService.getById(orderId);
                if ("UNPAID".equals(orderInfo.getOrderStatus()) || "UNPAID".equals(orderInfo.getProcessStatus())){
                    //  调用方法更新状态！
                    orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
                    //  发送消息给库存！
                    orderService.sendOrderStatus(orderId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //  手动确认消费
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  监听减库存消息： 如果没有队列，则会报错！ 所以，需要使用绑定方式！
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void wareOrder(String strJson, Message message,Channel channel){
        try {
            //  判断
            if(!StringUtils.isEmpty(strJson)){
                //  json 转换为Map
                Map map = JSON.parseObject(strJson, Map.class);
                //  获取到订单Id
                String orderId = (String) map.get("orderId");
                String status = (String) map.get("status");

                //  继续判断
                if("DEDUCTED".equals(status)){
                    //  减库存成功！
                    this.orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
                }else {
                    //  减库存失败！超卖！
                    //  记录日志..... 补货.....!  人工客服沟通解决.....!
                    this.orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        //  手动确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
}