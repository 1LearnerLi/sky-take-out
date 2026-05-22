package com.sky.service.impl;

import ch.qos.logback.core.status.StatusManager;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐与套餐菜品关系
     *
     * @param setmealDTO
     */
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        //setmealDTO数据分为两部分，一部分用于新增套餐，一部分用于新增套餐菜品关系

        //新增套餐
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        //新增套餐菜品关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmeal.getId());
            }
            setmealDishMapper.insert(setmealDishes);
        }
    }

    /**
     * 分页查询
     *
     * @return
     */
    public PageResult PageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        Page<SetmealVO> page = new Page<>(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        setmealMapper.pageQuery(page, setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getRecords());
    }

    /**
     * 批量删除套餐
     *
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断是否包含起售中的套餐
        List<Setmeal> setmeals = setmealMapper.selectByIds(ids);
        if (setmeals != null && setmeals.size() > 0) {
            for (Setmeal setmeal : setmeals) {
                if (setmeal.getStatus() == StatusConstant.ENABLE) {
                    throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
                }
            }
        }

        //删除套餐表对应数据
        setmealMapper.deleteBatch(ids);
        //删除套餐菜品关系表对应数据
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     *
     */
    public void updateWithSetmealDish() {

    }

    /**
     * 根据id查询套餐
     */
    @Transactional
    public SetmealVO getgetSetmealById(Long id) {
        //根据套餐Id查询套餐
        Setmeal setmeal = setmealMapper.selectById(id);
        //根据套餐Id查询套餐菜品关系表
        List<SetmealDish> setmealDishes = setmealDishMapper.selectBySetmealId(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Transactional
    public void updateWithSetmealDish(SetmealDTO setmealDTO) {
        //修改套餐表
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.updateById(setmeal);

        //修改套餐菜品关系表
        //1. 先删除与指定套餐Id关联的数据
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());

        //2. 再重新添加传来的所有数据
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmealDTO.getId());
            }
        }
        setmealDishMapper.insert(setmealDishes);
    }

    /**
     * 套餐起售、停售
     *
     * @param saleStatus
     * @param id
     */
    public void updateSaleStatus(Integer saleStatus, Long id) {
        //起售时，判断套餐内是否存在已停售菜品
        if (saleStatus == StatusConstant.ENABLE) {
            List<Integer> statuses = setmealDishMapper.selectStatusBySetmealId(id);
            if (statuses != null && statuses.size() > 0) {
                for (Integer status : statuses) {
                    if (status==StatusConstant.DISABLE){
                        throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                }
            }
        }
        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(saleStatus)
                .build();
        setmealMapper.updateById(setmeal);

    }

    /**
     * 条件查询
     * @param categoryId
     * @return
     */
    public List<Setmeal> list(Long categoryId) {
        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE);
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }


}
