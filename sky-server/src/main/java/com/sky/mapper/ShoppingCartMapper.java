package com.sky.mapper;

//import com.github.pagehelper.Page;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Category;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {

    /**
     * 购物车查询
     *
     * @param shoppingCart
     * @return
     */
    ShoppingCart list(ShoppingCart shoppingCart);

    /**
     * 按userId清空购物车
     * @param userId
     */
    @Delete("delete from shopping_cart where user_id=#{userId};")
    void deleteByUserId(Long userId);
}
