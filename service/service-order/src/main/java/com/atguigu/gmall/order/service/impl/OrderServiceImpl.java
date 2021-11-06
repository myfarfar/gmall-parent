package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;


    @Value("${ware.url}")
    private String WARE_URL;

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        // 远程调用http://localhost:9001/hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrderInfo(OrderInfo orderInfo) {
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(new Date());
        // 定义为1天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());

        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        // 获取订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuffer tradeBody = new StringBuffer();
        for (OrderDetail orderDetail : orderDetailList) {
            tradeBody.append(orderDetail.getSkuName()+" ");
        }
        if (tradeBody.toString().length()>100){
            orderInfo.setTradeBody(tradeBody.toString().substring(0,100));
        }else {
            orderInfo.setTradeBody(tradeBody.toString());
        }

        orderInfoMapper.insert(orderInfo);

        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }
//测试订单取消时间
//        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), 10);
//真实订单过期时间
                rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);
        return orderInfo.getId();
    }

    public String getTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 定义一个流水号
        String tradeNo = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(tradeNoKey, tradeNo);
        return tradeNo;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeCodeNo.equals(redisTradeNo);
    }

    @Override
    public void deleteTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 删除数据
        redisTemplate.delete(tradeNoKey);
    }


    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        QueryWrapper<OrderDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(queryWrapper);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }


    @Override
    public void execExpiredOrder(Long orderId) {
        // orderInfo
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
    }
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfoMapper.updateById(orderInfo);
    }


    @Override
    public void sendOrderStatus(Long orderId) {
        //  发送消息给库存了
        this.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        //  参考库存系统接口！   发送是Json 字符串！
        //  根据订单Id 来获取到订单orderInfo 对象，
        OrderInfo orderInfo = this.getOrderInfo(orderId);

        //  将orderInfo 中的部分字段转换为Map 集合
        Map map = this.initWare(orderInfo);

        //  将map 转换为Json 字符串，发送给库存！
        this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK, JSON.toJSONString(map));
    }

    public Map initWare(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        //  第一种：
        //        List<Map> list = new ArrayList<>();
        //        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //        for (OrderDetail orderDetail : orderDetailList) {
        //            //  声明一个map 集合
        //            HashMap<String, Object> detailMap = new HashMap<>();
        //            detailMap.put("skuId",orderDetail.getSkuId());
        //            detailMap.put("skuName",orderDetail.getSkuName());
        //            detailMap.put("skuNum",orderDetail.getSkuNum());
        //            //  添加到这个map;
        //            list.add(detailMap);
        //        }

        //  第二种写法：
        List<HashMap<String, Object>> list = orderInfo.getOrderDetailList().stream().map(orderDetail -> {
            //  声明一个map 集合
            HashMap<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId", orderDetail.getSkuId());
            detailMap.put("skuName", orderDetail.getSkuName());
            detailMap.put("skuNum", orderDetail.getSkuNum());
            return detailMap;
        }).collect(Collectors.toList());

        map.put("details",list); // details:[{skuId:101,skuNum:1,skuName:’小米手64G’},{skuId:201,skuNum:1,skuName:’索尼耳机’}]
        //  返回map
        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        /*
        1.  知道谁被拆单，先获取到原始订单是谁！
        2.  wareSkuMap  [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}] 转换为java 操作的对象
        3.  创建新的子订单，并给子订单赋值
        4.  子订单保存到数据库orderInfo
        5.  将新的子订单添加到子订单集合
        6.  修改原始订单状态
         */
        List<OrderInfo> subOrderInfoList = new ArrayList<>();

        OrderInfo orderInfoOrigin = this.getOrderInfo(Long.parseLong(orderId));
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        //  判断
        if (!CollectionUtils.isEmpty(mapList)){
            //  循环遍历
            for (Map map : mapList) {
                String wareId = (String) map.get("wareId");
                List<String> skuIdList = (List<String>) map.get("skuIds");
                //  创建子订单对象
                OrderInfo subOrderInfo = new OrderInfo();
                //  属性拷贝
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                //  需要做细节处理：
                subOrderInfo.setId(null);
                //  父订单Id
                subOrderInfo.setParentOrderId(Long.parseLong(orderId));
                //  赋值仓库Id
                subOrderInfo.setWareId(wareId);
                //  子订单金额计算！
                //  声明一个子订单明细集合：
                List<OrderDetail> orderDetails = new ArrayList<>();
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                for (OrderDetail orderDetail : orderDetailList) {
                    //  找到属于子订单的订单明细集合
                    for (String skuId : skuIdList) {
                        if (orderDetail.getSkuId()== Long.parseLong(skuId)){
                            orderDetails.add(orderDetail);
                        }
                    }
                }

                //  计算子订单的价格：
                subOrderInfo.setOrderDetailList(orderDetails);
                subOrderInfo.sumTotalAmount();
                //  保存子订单数据
                this.saveOrderInfo(subOrderInfo);
                //  将子订单添加到集合中！
                subOrderInfoList.add(subOrderInfo);
            }
        }
        //  修改原始订单状态
        this.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.SPLIT);
        return subOrderInfoList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        //  关闭订单orderInfo
        this.updateOrderStatus(orderId,ProcessStatus.CLOSED);
        //  判断
        if ("2".equals(flag)){
            //  发送消息：
            this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }

    }

    @Override
    public void updateCommnetStatus(Long orderId) {
        OrderInfo orderInfo = this.getById(orderId);
        orderInfo.setOrderStatus(ProcessStatus.COMMNET.getOrderStatus().name());
        orderInfo.setProcessStatus(ProcessStatus.COMMNET.name());
        this.updateById(orderInfo);
    }

}
