package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SetmealDishMapper extends BaseMapper<SetmealDish> {
    /**
     * 根据菜品id查询套餐id
     * @param dishIds
     * @return
     */
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    /**
     * 根据多个套餐id删除套餐菜品关系表中与套餐关联的数据
     * @param setmealIds
     */
    void deleteBySetmealIds(List<Long> setmealIds);

    /**
     * 根据一个套餐id删除套餐菜品关系表中与套餐关联的数据
     * @param setmealId
     */
    @Delete("delete from setmeal_dish where setmeal_id=#{setmealId};")
    void deleteBySetmealId(Long setmealId);

    /**
     * 据套餐Id查询套餐菜品关系表与菜品表
     * @param setmealId
     * @return
     */
    List<Integer> selectStatusBySetmealId (Long setmealId);

    /**
     * 据套餐Id查询套餐菜品关系表
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id=#{setmealId};")
    List<SetmealDish> selectBySetmealId(Long setmealId);
}
