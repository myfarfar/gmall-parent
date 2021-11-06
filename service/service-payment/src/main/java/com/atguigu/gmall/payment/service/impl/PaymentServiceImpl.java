package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        //  这个订单，是否只允许使用一种方式支付成功！
        //  yes! {实时记录当前这个订单Id 与订单的状态！ 每次在要生产二维码的时候，都需要进行查询}
        //  no yes!  此时，你只需要一个操作了！退款！
        //
        //  判断：
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",orderInfo.getOutTradeNo());
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        if (paymentInfoQuery!=null){
            return;
        }

        //  创建一个空对象
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        //  这个实体类中没有...自己添加的！
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());

        //  直接插入数据：
        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {
        //  select * from payment_info where  out_trade_no = ? and payment_type=?
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        return paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
    }

    @Override
    public void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramMap) {

        //  PAID,或者 CLOSED 则不走了！
        PaymentInfo paymentInfoQuery = this.getPaymentInfo(outTradeNo, paymentType);
        //        if ("PAID".equals(paymentInfoQuery.getPaymentStatus())
        //            || "CLOSED".equals(paymentInfoQuery.getPaymentStatus())){
        //            return;
        //        }
        //  本质：更新paymentInfo.payment_status
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setTradeNo(paramMap.get("trade_no"));
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(paramMap.toString());
        //  第一个参数： 存储要更新的数据值，第二个参数：更新的条件
        //        UpdateWrapper<PaymentInfo> paymentInfoUpdateWrapper = new UpdateWrapper<>();
        //        paymentInfoUpdateWrapper.eq("out_trade_no",outTradeNo);
        //        paymentInfoUpdateWrapper.eq("payment_type",paymentType);
        //        //  new QueryWrapper<>();
        //        this.paymentInfoMapper.update(paymentInfo,paymentInfoUpdateWrapper);
        //  将这个方法进行抽取：
        this.updatePaymentInfo(outTradeNo,paymentType,paymentInfo);

        //  发送一个异步消息：通知订单，然后修改订单状态！异步操作!   还可以通过远程调用：同步操作了
        this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());

    }
    //  更新交易记录状态！
    public void updatePaymentInfo(String outTradeNo, String paymentType, PaymentInfo paymentInfo) {
        //  第一个参数： 存储要更新的数据值，第二个参数：更新的条件
        UpdateWrapper<PaymentInfo> paymentInfoUpdateWrapper = new UpdateWrapper<>();
        paymentInfoUpdateWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoUpdateWrapper.eq("payment_type",paymentType);
        this.paymentInfoMapper.update(paymentInfo,paymentInfoUpdateWrapper);
    }

    @Override
    public void closePayment(Long orderId) {
        //  select * from payment_info where order_id = ?;
        UpdateWrapper<PaymentInfo> paymentInfoUpdateWrapper = new UpdateWrapper<>();
        paymentInfoUpdateWrapper.eq("order_id",orderId);
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoUpdateWrapper);
        if (paymentInfoQuery==null) return;

        //  update payment_info set payment_status = ? where order_id = ?
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
        paymentInfoMapper.update(paymentInfo,paymentInfoUpdateWrapper);

    }
}