package com.cvs.mapper;

import com.cvs.annotation.AutoFill;
import com.cvs.entity.Dish;
import com.cvs.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);
    @AutoFill(OperationType.INSERT)
    void insert(Dish dish);
}