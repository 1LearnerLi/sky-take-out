package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    /**
     * 新增套餐
     * @param setmealDTO
     */
    void save(SetmealDTO setmealDTO);

    /**
     * 分页查询
     * @return
     */
    PageResult PageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 批量删除套餐
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询套餐
     * @return
     */
    SetmealVO getgetSetmealById(Long id);

    /**
     * 修改套餐
     */
    void updateWithSetmealDish(SetmealDTO setmealDTO);

    /**
     * 套餐起售、停售
     * @param saleStatus
     * @param id
     */
    void updateSaleStatus(Integer saleStatus, Long id);
}
