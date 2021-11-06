package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {


    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */

    OrderInfo getOrderInfo(Long orderId);

    /**
     * 处理过期订单
     * @param orderId
     */
    void execExpiredOrder(Long orderId);
    /**
     * 根据订单Id 修改订单的状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);


    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    Long saveOrderInfo(OrderInfo orderInfo);


    /**
     * 生产流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param userId 获取缓存中的流水号
     * @param tradeCodeNo   页面传递过来的流水号
     * @return
     */
    boolean checkTradeCode(String userId, String tradeCodeNo);


    /**
     * 删除流水号
     * @param userId
     */
    void deleteTradeNo(String userId);

    boolean checkStock(Long skuId, Integer skuNum);
    /**
     * 发送消息给库存！
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * 将orderInfo 转换为map 集合
     * @param orderInfo
     * @return
     */
    Map initWare(OrderInfo orderInfo);

    /**
     * 拆单的方法
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);

    /**
     * 关闭记录
     * @param orderId
     * @param flag
     */
    void execExpiredOrder(Long orderId, String flag);


    void updateCommnetStatus(Long orderId);
}
