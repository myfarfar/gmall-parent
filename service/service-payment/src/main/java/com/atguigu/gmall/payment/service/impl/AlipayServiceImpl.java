package com.atguigu.gmall.payment.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentService paymentService;


    @Override
    public String createaliPay(Long orderId) throws AlipayApiException {
        //  获取订单对象
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //  判断 关闭状态，或者是已经支付了，则不能再生产二维码！
        if ("CLOSED".equals(orderInfo.getOrderStatus()) || "PAID".equals(orderInfo.getOrderStatus())){
            return "订单已关闭或者订单已支付！";
        }
        //  保存交易记录
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());
        //  AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest(); //创建API对应的request
        //  同步回调地址：
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);

        //  异步回调地址：
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url); //在公共参数中设置回跳和通知地址

        //  看api 文档
        //        HashMap<String, Object> map = new HashMap<>();
        //        map.put("out_trade_no",orderInfo.getOutTradeNo());
        //        alipayRequest.setBizContent(JSON.toJSONString(map));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("out_trade_no",orderInfo.getOutTradeNo());
        //  jsonObject.put("total_amount",orderInfo.getTotalAmount());
        jsonObject.put("total_amount","0.01");
        jsonObject.put("subject",orderInfo.getTradeBody());
        jsonObject.put("product_code","FAST_INSTANT_TRADE_PAY");
        //  SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        //  时间相加：
        //  Calendar calendar = Calendar.getInstance();
        //  calendar.add(Calendar.HOUR,1);
        //  orderInfo.setExpireTime(calendar.getTime()); // +1天
        //  String format = simpleDateFormat.format(calendar.getTime());
        //  Date expireTime = orderInfo.getExpireTime();
        //  jsonObject.put("time_expire",simpleDateFormat.format(expireTime));
        jsonObject.put("timeout_express","10m");

        alipayRequest.setBizContent(jsonObject.toJSONString());
        //  返回数据
        return alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单

    }

    @Override
    public Boolean refund(Long orderId) {
        //  通过orderId 来获取到OrderInfo
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", 0.01);
        bizContent.put("out_request_no", "HZ01RF001");

        request.setBizContent(bizContent.toString());
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            //  退款：
            //  修改订单状态 , 交易记录状态！
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
            this.paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(),PaymentType.ALIPAY.name(),paymentInfo);
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public Boolean closePay(Long orderId) {
        //  根据orderId 来获取数据
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);
        //  调用接口方法
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no",orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public Boolean checkPayment(Long orderId) {
        //  根据orderId 来获取数据
        OrderInfo orderInfo = this.orderFeignClient.getOrderInfo(orderId);

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;

        } else {
            System.out.println("调用失败");
            return false;
        }
    }
}
