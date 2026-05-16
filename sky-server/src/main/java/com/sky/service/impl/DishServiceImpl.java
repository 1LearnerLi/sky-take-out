package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应的口味
     *
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //向餐品表中插入一条数据
        dishMapper.insert(dish);

        Long dishId = dish.getId();

        //向口味表中插入0,1或多条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });
            dishFlavorMapper.insert(flavors);

        }

    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {

        Page<DishVO> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());


        dishMapper.pageQuery(page, dishPageQueryDTO);

        return new PageResult(page.getTotal(), page.getRecords());
    }

    /**
     * 菜品批量删除
     *
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判断当前菜品是否能够删除：     是否存在起售中的菜品

        List<Dish> dishes = dishMapper.selectByIds(ids);
        for (Dish dish : dishes) {
            if (dish.getStatus()==StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);

            }
        }

        /*for (Long id : ids) {
            Dish dish = dishMapper.selectById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
            }
        }*/

        //判断当前菜品是否能够删除：     是否被套餐关联

        List<Long> SetmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (SetmealIds != null && SetmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品表中的菜品数据

        dishMapper.deleteByIds(ids);

        //删除口味表中与菜品关联的口味数据
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据Id查询菜品
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {

        //按id查dish表
        Dish dish = dishMapper.selectById(id);

        //按id查dishFlavor表
        List<DishFlavor> dishFlavor=dishFlavorMapper.selectByDishId(id);

        //查询到的所有数据封装进dishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavor);

        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     * @return
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        //基于设计理念，使用与dish表贴合的Dish类进行修改dish表操作
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.updateById(dish);

        //dish_flavor表的修改操作
        //先将指定dish_id的所有口味数据删除，再重新将传进来的口味数据添加
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insert(flavors);

        }


    }


}
