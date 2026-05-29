package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;

    /**
     * 定时处理超时订单
     */
    @Scheduled(cron = "0 * * * * ?")    //每分钟触发一次
    public void processTimeOutOrder() {
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        //status==1,order_time + 15 < 当前时间
        wrapper.eq(Orders::getStatus, Orders.PENDING_PAYMENT)
                .lt(Orders::getOrderTime, LocalDateTime.now().plusMinutes(-15));
        List<Orders> orders = orderMapper.selectList(wrapper);
        if (orders != null && orders.size() > 0) {
            for (Orders order : orders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("订单超时，自动取消");
            }
        }
        orderMapper.updateById(orders);

    }

    /**
     * 定时处理处于派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ?")   //每天凌晨1点触发一次
    public void processDeliveryOrder() {
        log.info("定时处理处于派送中的订单：{}", LocalDateTime.now());
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        //本质上是检查前一天的所有订单（凌晨一点早打烊了，该送到的都送到了）  ：order_time < 当前时间（1点）- 60分
        //status==4
        wrapper.eq(Orders::getStatus, Orders.DELIVERY_IN_PROGRESS)
                .lt(Orders::getOrderTime, LocalDateTime.now().plusMinutes(-60));
        List<Orders> orders = orderMapper.selectList(wrapper);
        if (orders != null && orders.size() > 0) {
            for (Orders order : orders) {
                order.setStatus(Orders.COMPLETED);
            }
        }
        orderMapper.updateById(orders);
    }

}
