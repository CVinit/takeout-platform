package com.cvs.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cvs.constant.MessageConstant;
import com.cvs.context.BaseContext;
import com.cvs.delayqueue.OrderDelayQueue;
import com.cvs.delayqueue.OrdersInDelay;
import com.cvs.dto.*;
import com.cvs.entity.*;
import com.cvs.exception.AddressBookBusinessException;
import com.cvs.exception.OrderBusinessException;
import com.cvs.exception.ShoppingCartBusinessException;
import com.cvs.mapper.*;
import com.cvs.properties.ShopAddressProperties;
import com.cvs.result.PageResult;
import com.cvs.service.OrderService;
import com.cvs.utils.MapUtil;
import com.cvs.utils.WeChatPayUtil;
import com.cvs.vo.OrderPaymentVO;
import com.cvs.vo.OrderStatisticsVO;
import com.cvs.vo.OrderSubmitVO;
import com.cvs.vo.OrderVO;
import com.cvs.websocket.WebSocketServer;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.DelayQueue;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private MapUtil mapUtil;
    @Autowired
    private ShopAddressProperties shopAddressProperties;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private WebSocketServer webSocketServer;
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //1. 处理各种业务异常（地址簿为空，购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        String userAddress = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail();

        ValueOperations valueOperations = redisTemplate.opsForValue();
        String shopLocation = "";
        String shopLocationCache = (String) valueOperations.get("shopLocation");
        if (shopLocationCache == null || shopLocationCache.equals("")){
            shopLocation = mapUtil.getLocation(shopAddressProperties.getAddress());
            valueOperations.set("shopLocation",shopLocation);
        }else {
            shopLocation = shopLocationCache;
        }
        String userLocation = mapUtil.getLocation(userAddress);

        Long distance = mapUtil.getDistance(shopLocation, userLocation);
        if (distance.longValue() > 5000){
            throw new OrderBusinessException("距离过远无法配送");
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder().id(userId).build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.size() == 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2. 向订单表插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(userAddress);
        User user = userMapper.getById(userId);
        orders.setUserName(user.getName());
        orders.setUserId(userId);

        orderMapper.insert(orders);

        //3. 向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        BigDecimal amount = new BigDecimal(0);
        for (ShoppingCart cart : list){
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            amount = amount.add(cart.getAmount());
            orderDetailList.add(orderDetail);
        }
        //重新计算金额并更新到表中，保证总金额正确
        amount = amount.add(new BigDecimal(orders.getPackAmount()+6));
        orderMapper.update(orders);
        //批量插入订单明细数据
        orderDetailMapper.insertBatch(orderDetailList);
        //4. 清空当前购物车数据

        shoppingCartMapper.deleteByUserId(userId);

        //5. 封装VO返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(LocalDateTime.now())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        //6. 向OrderDelayQueue中添加此订单
        OrdersInDelay ordersInDelay = new OrdersInDelay();
        ordersInDelay.setId(orders.getId());
        ordersInDelay.setTime(orders.getOrderTime().plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        OrderDelayQueue.getInstance().add(ordersInDelay);
        log.info("向延迟队列中添加订单：{}",orders.getId());

        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        //不使用微信支付。返回支付成功数据
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code","ORDERPAID");

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
        vo.setPackageStr("prepay_id=wx201410272009395522657a690389285100"); //预支付id

        //支付成功修改订单状态
        paySuccess(ordersPaymentDTO.getOrderNumber());


        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通过websocket向客户端推送消息
        HashMap<String, Object> map = new HashMap<>();
        map.put("type",1);
        map.put("orderId",ordersDB.getId());
        map.put("content","订单号：" + outTradeNo);

        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款
     * @return
     */
    @Override
    public PageResult pageQuery4User(Integer page, Integer pageSize, Integer status) {
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        PageHelper.startPage(page, pageSize);
        Page<Orders> ordersPage = orderMapper.pageQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = new ArrayList<>();
        if (ordersPage != null && ordersPage.size() > 0){
            for (Orders orders:ordersPage){
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
                orderVO.setOrderDetailList(orderDetailList);
                orderVOList.add(orderVO);
            }
        }

        return new PageResult(ordersPage.getTotal(),orderVOList);
    }

    /**
     * 根据id查询订单及明细
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetailById(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
        orderVO.setOrderDetailList(orderDetails);

        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void cancelOrder(Long id, String cancelReason) {
        Orders order = orderMapper.getById(id);

        if (order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (order.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orderNew = new Orders();
        orderNew.setId(id);
        if (order.getStatus().equals(Orders.TO_BE_CONFIRMED) || order.getPayStatus().equals(Orders.PAID)){
            try {
                weChatPayUtil.refund(order.getNumber(),order.getNumber(),new BigDecimal(0.1),new BigDecimal(0.1));
            } catch (Exception e) {
                throw new OrderBusinessException("退款失败：" + e.toString());
            }
            orderNew.setPayStatus(Orders.REFUND);
        }
        orderNew.setStatus(Orders.CANCELLED);
        if (cancelReason == null || cancelReason.length() <= 0){
            orderNew.setCancelReason("用户取消");
        }else {
            orderNew.setCancelReason(cancelReason);
        }
        orderNew.setCancelTime(LocalDateTime.now());
        orderMapper.update(orderNew);
    }

    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);
        List<ShoppingCart> shoppingCartList = new ArrayList<>();
        if (orderDetails != null && orderDetails.size() > 0){
            for (OrderDetail orderDetail:orderDetails){
                ShoppingCart shoppingCart = new ShoppingCart();
                BeanUtils.copyProperties(orderDetail,shoppingCart);
                shoppingCart.setUserId(BaseContext.getCurrentId());
                shoppingCart.setCreateTime(LocalDateTime.now());
                shoppingCartList.add(shoppingCart);
            }
        }
        shoppingCartMapper.insertBatch(shoppingCartList);


    }

    /**
     * 管理端订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery4Admin(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(),ordersPageQueryDTO.getPageSize());

        Page<Orders> ordersPage = orderMapper.pageQuery(ordersPageQueryDTO);

        if (ordersPage == null || ordersPage.size() <= 0){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        List<OrderVO> orderVOList = new ArrayList<>();

        for (Orders order:ordersPage){
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(order,orderVO);
            List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(order.getId());
            if (orderDetails == null || orderDetails.size() <= 0){
                throw new OrderBusinessException("当前订单中没有菜品");
            }
            StringBuffer orderDishesSB = new StringBuffer();
            for (OrderDetail orderDetail:orderDetails){
                orderDishesSB.append(orderDetail.getName() + "*" + orderDetail.getNumber() + ",");
            }
            if (orderDishesSB.length() > 0){
                orderDishesSB.deleteCharAt(orderDishesSB.length() - 1).append(";");
            }
            orderVO.setOrderDishes(orderDishesSB.toString());
            orderVOList.add(orderVO);
        }

        return new PageResult(ordersPage.getTotal(),orderVOList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);


        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders order = orderMapper.getById(ordersConfirmDTO.getId());

        if (order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (!order.getStatus().equals(2)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orderNew = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orderNew);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders order = orderMapper.getById(ordersRejectionDTO.getId());

        if (order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (order.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
//        if (order.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
//            try {
//                weChatPayUtil.refund(order.getNumber(), order.getNumber(), order.getAmount(), new BigDecimal("0.01"));
//            } catch (Exception e) {
//                throw new OrderBusinessException("退款失败:" + e.toString());
//            }
//        }
        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .payStatus(Orders.REFUND)
                .build();
        orderMapper.update(orders);

    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Long id) {
        Orders order = orderMapper.getById(id);

        if (order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (!order.getStatus().equals(3)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders order = orderMapper.getById(id);

        if (order == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (!order.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);

    }

    /**
     * 客户催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        Orders ordersDB = orderMapper.getById(id);

        if (ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        if (ordersDB.getPayStatus().equals(Orders.PAID) && ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            HashMap<String, Object> map = new HashMap<>();
            map.put("type",2);
            map.put("orderId",id);
            map.put("content","订单号：" + ordersDB.getNumber());

            String jsonString = JSON.toJSONString(map);
            webSocketServer.sendToAllClient(jsonString);
        }

    }
}
