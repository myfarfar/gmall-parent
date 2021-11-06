package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.client.ProductFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();

        // 定义key user:userId:cart
        String cartKey = this.getCartKey(userId);
        List<CartInfo> cartCachInfoList = redisTemplate.opsForHash().values(cartKey);
        if (null != cartCachInfoList && cartCachInfoList.size() > 0) {
            for (CartInfo cartInfo : cartCachInfoList) {
                // 获取选中的商品！
                if (cartInfo.getIsChecked().intValue() == 1) {
                    cartInfoList.add(cartInfo);
                }
            }
        }
        return cartInfoList;
    }


    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        // 什么一个返回的集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();

        // 未登录：临时用户Id 获取未登录的购物车数据
        if (StringUtils.isEmpty(userId)) {
            cartInfoList = this.getCartList(userTempId);
            return cartInfoList;
        }

    /*
         1. 准备合并购物车
         2. 获取未登录的购物车数据
         3. 如果未登录购物车中有数据，则进行合并 合并的条件：skuId 相同 则数量相加，合并完成之后，删除未登录的数据！
         4. 如果未登录购物车没有数据，则直接显示已登录的数据
          */
        //已登录
        if (!StringUtils.isEmpty(userId)) {
            List<CartInfo> cartInfoArrayList = this.getCartList(userTempId);
            if (!CollectionUtils.isEmpty(cartInfoArrayList)) {
                // 如果未登录购物车中有数据，则进行合并 合并的条件：skuId 相同
                cartInfoList = this.mergeToCartList(cartInfoArrayList, userId);
                // 删除未登录购物车数据
                this.deleteCartList(userTempId);
            }

            // 如果未登录购物车中没用数据！
            if (StringUtils.isEmpty(userTempId) || CollectionUtils.isEmpty(cartInfoArrayList)) {
                // 根据什么查询？userId
                cartInfoList = this.getCartList(userId);
            }
        }
        return cartInfoList;
    }

    /**
     * 合并
     * @param userId
     * @return
     */
    private List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId) {
     /*
    demo1:
        登录：
            37 1
            38 1
        未登录：
            37 1
            38 1
            39 1
        合并之后的数据
            37 2
            38 2
            39 1
         */
        //  根据用户 Id 查询登录的购物车数据
        List<CartInfo> cartInfoLoginList = this.getCartList(userId);
        //  登录购物车数据要是空，则直接返回{需要将未登录的数据添加到数据库}
        //  if (CollectionUtils.isEmpty(cartInfoLoginList)) return cartInfoNoLoginList;
        //  登录的购物车数据不为空！ 合并条件skuId

        Map<Long, CartInfo> longCartInfoMap = cartInfoLoginList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        //  判断未登录的skuId 在已登录的Map 中是否有这个key
        //  遍历集合
        for (CartInfo cartInfo : cartInfoNoLoginList) {
            Long skuId = cartInfo.getSkuId();
            //  skuId 是否存在  37,38
            if (longCartInfoMap.containsKey(skuId)){
                //  未登录购物车中的商品，在登录中也存在！数量相加
                //  用这个skuId 对应获取登录的cartInfo 对象
                CartInfo cartInfoLogin = longCartInfoMap.get(skuId);
                //  赋值商品数量
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfo.getSkuNum());
                //  更新一下cartInfo 的更新时间
                cartInfoLogin.setUpdateTime(new Timestamp(new Date().getTime()));

                //  合并购物车时的选择状态！
                //  以未登录选中状态为基准
                if (cartInfo.getIsChecked().intValue()==1){
                    cartInfoLogin.setIsChecked(1);
                }
                //  更新数据
                //  使用 update cart_info set sku_num = ? where sku_id = ? and user_id = ?
                QueryWrapper<CartInfo> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("user_id",cartInfoLogin.getUserId());
                queryWrapper.eq("sku_id",cartInfoLogin.getSkuId());
                //  cartInfo第一个参数相当于 ，第二个参数相当于更新条件
                cartInfoMapper.update(cartInfoLogin,queryWrapper);
            }else {
                //  处理39
                //  赋值登录的用户id
                cartInfo.setUserId(userId);
                //  添加时间
                cartInfo.setCreateTime(new Timestamp(new Date().getTime()));
                cartInfo.setUpdateTime(new Timestamp(new Date().getTime()));
                cartInfoMapper.insert(cartInfo);
            }
        }

        //  从数据库中获取到最新合并的数据，然后放入缓存
        List<CartInfo> cartInfoList = this.loadCartCache(userId);
        //  返回数据
        return cartInfoList;
    }


    /**
     * 删除购物车
     * @param userTempId
     */
    private void deleteCartList(String userTempId) {
        // 删除数据库，删除缓存
        // delete from userInfo where userId = ?userTempId
        QueryWrapper queryWrapper = new QueryWrapper<CartInfo>();
        queryWrapper.eq("user_id", userTempId);
        cartInfoMapper.delete(queryWrapper);
        String cartKey = getCartKey(userTempId);

        Boolean flag = redisTemplate.hasKey(cartKey);
        if (flag){
            redisTemplate.delete(cartKey);
        }
    }



    /**
     * 根据用户获取购物车
     * @param userId
     * @return
     */
    private List<CartInfo> getCartList(String userId) {
        // 声明一个返回的集合对象
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (StringUtils.isEmpty(userId)) return cartInfoList;

    /*
    1.  根据用户Id 查询 {先查询缓存，缓存没有，再查询数据库}
     */
        // 定义key user:userId:cart
        String cartKey = this.getCartKey(userId);
        // 获取数据
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        if (!CollectionUtils.isEmpty(cartInfoList)) {
            // 购物车列表显示有顺序：按照商品的更新时间 降序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                   return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(), Calendar.SECOND);
                }
            });
            return cartInfoList;
        } else {
            // 缓存中没用数据！
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    /**
     * 通过userId 查询购物车并放入缓存！
     * @param userId
     * @return
     */
    public List<CartInfo> loadCartCache(String userId) {
        QueryWrapper queryWrapper = new QueryWrapper<CartInfo>();
        queryWrapper.eq("user_id", userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(cartInfoList)) {
            return cartInfoList;
        }
        // 将数据库中的数据查询并放入缓存
        HashMap<String, CartInfo> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(cartInfo.getSkuId());
            cartInfo.setSkuPrice(skuPrice);
            map.put(cartInfo.getSkuId().toString(), cartInfo);
        }

        // 定义key user:userId:cart
        String cartKey = this.getCartKey(userId);
        redisTemplate.opsForHash().putAll(cartKey, map);
        // 设置过期时间
        this.setCartKeyExpire(cartKey);
        return cartInfoList;
    }




    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /*
            1.  添加商品之前，先看一下购物车中是否有该商品
                true:
                    商品数量相加
                false:
                    直接加入购物车
            2.    将数据同步到redis！

         */
        //  数据类型hash + key hset(key,field,value)
        //  key = user:userId:cart ,谁的购物车 field = skuId value = cartInfo
        String cartKey = this.getCartKey(userId);
        //  判断缓存中是否有数据
        if (!redisTemplate.hasKey(cartKey)){
            //  加载数据库并放入缓存
            this.loadCartCache(userId);
        }
        //从缓存查询购物车内容
        CartInfo cartInfoExist = (CartInfo) redisTemplate.opsForHash().get(cartKey, skuId.toString());
        //  select * from cart_info where skuId = ? and userId = ?;
 /*       QueryWrapper<CartInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        queryWrapper.eq("sku_id",skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(queryWrapper);*/
        //  当前购物车中有该商品
        if (cartInfoExist!=null){
            //  数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //  初始化实时价格
            //  本质skuPrice = skuInfo.price
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            //  修改更新时间
            cartInfoExist.setUpdateTime(new Timestamp(new Date().getTime()));
            //  再次添加商品时，默认选中状态。
            cartInfoExist.setIsChecked(1);

            //  修改数据库执行语句
            cartInfoMapper.updateById(cartInfoExist);

        }else {
            //  第一次添加购物车
            CartInfo cartInfo = new CartInfo();
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            //  在初始化的时候，添加购物车的价格 = skuInfo.price
            cartInfo.setCartPrice(skuInfo.getPrice());
            //  数据库不存在的，购物车的价格 = skuInfo.price
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setCreateTime(new Timestamp(new Date().getTime()));
            cartInfo.setUpdateTime(new Timestamp(new Date().getTime()));
            //  执行数据库操作
            cartInfoMapper.insert(cartInfo);

            cartInfoExist = cartInfo;
        }

        //  存储数据 hset(key,field,value);
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);
        //  设置过期时间
        this.setCartKeyExpire(cartKey);
    }
    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        // update cartInfo set isChecked=? where  skuId = ? and userId=？
        // 修改数据库
        // 第一个参数表示修改的数据，第二个参数表示条件
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper queryWrapper = new QueryWrapper<CartInfo>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("sku_id", skuId);
        cartInfoMapper.update(cartInfo, queryWrapper);

        // 修改缓存
        // 定义key user:userId:cart
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String, String, CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        // 先获取用户选择的商品
        if (hashOperations.hasKey(skuId.toString())) {
            CartInfo cartInfoUpd = hashOperations.get(skuId.toString());
            // cartInfoUpd 写会缓存
            cartInfoUpd.setIsChecked(isChecked);

            // 更新缓存
            hashOperations.put(skuId.toString(), cartInfoUpd);
            // 设置过期时间
            this.setCartKeyExpire(cartKey);
        }
    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        String cartKey = getCartKey(userId);
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id", userId).eq("sku_id", skuId));

        //获取缓存对象
        BoundHashOperations<String, String, CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        if (hashOperations.hasKey(skuId.toString())){
            hashOperations.delete(skuId.toString());
        }
    }



    // 设置过期时间
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
    // 获取购物车的key
    private String getCartKey(String userId) {
        //定义key user:userId:cart
        return RedisConst.USER_KEY_PREFIX + userId +       RedisConst.USER_CART_KEY_SUFFIX;
    }

}