package com.cvs.service;

import com.cvs.dto.DishDTO;
import com.cvs.dto.DishPageQueryDTO;
import com.cvs.result.PageResult;
import com.cvs.vo.DishVO;

import java.util.List;

public interface DishService {

    /**
     * 新增菜品和对应的口味数据
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO);

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 菜品批量删除
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询菜品及口味
     * @param id
     * @return
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 更新菜品及口味
     * @param dishDTO
     */
    void updateDishWithFlavor(DishDTO dishDTO);

    /**
     * 起售或禁售菜品
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);
}
