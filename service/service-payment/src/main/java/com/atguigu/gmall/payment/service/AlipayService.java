package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

public interface AlipayService {


    //  要考虑返回值，参数！
    //  httpResponse.getWriter().write(form); 将表的写入到页面！
    String createaliPay(Long orderId) throws AlipayApiException;

    //  退款
    Boolean refund(Long orderId);

    //  关闭支付宝交易记录
    Boolean closePay(Long orderId);

    /**
     * 根据订单查询是否支付成功！
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);
}

