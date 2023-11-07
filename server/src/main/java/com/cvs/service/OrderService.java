package com.cvs.service;

import com.cvs.dto.OrdersPaymentDTO;
import com.cvs.dto.OrdersSubmitDTO;
import com.cvs.result.PageResult;
import com.cvs.vo.OrderPaymentVO;
import com.cvs.vo.OrderSubmitVO;
import com.cvs.vo.OrderVO;

public interface OrderService {

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 历史订单查询
     * @param page
     * @param pageSize
     * @param status 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消 7退款
     * @return
     */
    PageResult pageQuery4User(Integer page, Integer pageSize, Integer status);

    /**
     * 根据id查询订单及订单明细
     * @param id
     * @return
     */
    OrderVO getOrderDetailById(Long id);

    /**
     * 取消订单
     * @param id
     */
    void cancelOrder(Long id);

    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);
}
