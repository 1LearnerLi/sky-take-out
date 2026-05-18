package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

public interface DishFlavorMapper extends BaseMapper<DishFlavor> {
    /**
     * 根据单个dishId删除口味表中与菜品关联的口味数据
     * @param dishId
     */
    @Delete("delete from dish_flavor where dish_id=#{dishId};")
    void deleteByDishId(Long dishId);

    /**
     * 根据多个dishId删除口味表中与菜品关联的口味数据
     * @param dishIds
     */
    void deleteByDishIds(List<Long> dishIds);

    /**
     * 根据dish_id查询dish_flavor表
     * @param id
     * @return
     */
    List<DishFlavor> selectByDishId(Long id);



}
