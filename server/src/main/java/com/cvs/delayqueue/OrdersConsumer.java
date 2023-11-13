package com.cvs.delayqueue;

import com.cvs.entity.Orders;
import com.cvs.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.DelayQueue;

@Component
@Slf4j
public class OrdersConsumer implements Runnable{
    @Autowired
    private OrderMapper orderMapper;

    @PostConstruct
    public void init(){
        new Thread(this).start();
        log.info("延迟队列消费者线程启动......");
    }
    @Override
    public void run() {
        while (true){
            DelayQueue<OrdersInDelay> orderDelayQueue = OrderDelayQueue.getInstance();
            if (orderDelayQueue != null){
                OrdersInDelay ordersInDelay = orderDelayQueue.poll();
                if (ordersInDelay == null) continue;
                log.info("获取到延迟队列中的对象：{}",ordersInDelay);
                Orders orders = orderMapper.getById(ordersInDelay.getId());
                log.info("获取到待检订单：{}",orders.getId());
                if (orders.getPayStatus().equals(Orders.UN_PAID)) {
                    log.info("开始自动取消超时订单：{}",orders.getId());
                    Orders ordersToBeUpdated = Orders.builder()
                            .id(orders.getId())
                            .cancelTime(LocalDateTime.now())
                            .cancelReason("订单超时，自动取消")
                            .status(Orders.CANCELLED)
                            .build();
                    orderMapper.update(ordersToBeUpdated);
                }
            }

        }
    }
}
