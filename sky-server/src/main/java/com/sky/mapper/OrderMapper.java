package com.sky.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface OrderMapper extends BaseMapper<Orders> {
    /**
     * 管理端订单搜索
     * @param page
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> conditionSearch(@Param("page") Page<Orders> page,@Param("dto") OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据动态条件统计营业额（总金额）
     * @param map
     * @return
     */
    Double sumByMap(Map map);
}
