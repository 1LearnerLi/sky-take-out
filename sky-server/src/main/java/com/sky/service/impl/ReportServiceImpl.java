package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private OrderMapper orderMapper;


    /**
     * 统计指定时间区间内的营业额数据
     *
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //查找begin到end范围内所有日期
        //使用集合封装
        List<LocalDate> dateList = new ArrayList<>();
        //将范围内日期一个个封装
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //将集合转为字符串，并以逗号分割（lang包下的String.join方法也有相同作用）
        String date = StringUtils.join(dateList, ",");


        //封装每天营业额
        List<Double> turnoverList = new ArrayList<>();
        //查询每天的营业额
        for (LocalDate localDate : dateList) {
            //从每天的0点0分0秒
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            //到每天的23点59分59秒
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            //已完成的订单
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            //如果查询时符合条件的订单数为零，那么查询到的订单就为null，再去计算sum(amount)也就会为null，而不是0
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);

        }

        return TurnoverReportVO.builder()
                .dateList(date)
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();


    }
}
