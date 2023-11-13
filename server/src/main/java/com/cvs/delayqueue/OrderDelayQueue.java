package com.cvs.delayqueue;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.concurrent.DelayQueue;

@Data
public class OrderDelayQueue {

    private OrderDelayQueue(){

    }

    private static DelayQueue<OrdersInDelay> ordersInDelays = null;

    public static DelayQueue<OrdersInDelay> getInstance(){
        if (ordersInDelays == null){
            ordersInDelays = new DelayQueue<>();
        }

        return ordersInDelays;
    }
}
