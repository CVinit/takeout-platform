package com.cvs.service;

import com.cvs.annotation.AutoFill;
import com.cvs.dto.SetmealDTO;
import com.cvs.dto.SetmealPageQueryDTO;
import com.cvs.enumeration.OperationType;
import com.cvs.result.PageResult;
import com.cvs.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    /**
     * 新增套餐
     * @param setmealDTO
     */
    void saveWithDishes(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 批量删除菜品
     * @param ids
     */
    void deleteBatch(List<Long> ids);

    /**
     * 修改套餐
     * @param setmealDTO
     */
    void updateWithDish(SetmealDTO setmealDTO);

    /**
     * 根据id查询套餐及对应菜品
     * @param id
     * @return
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 起售、停售套餐
     * @param id
     * @param status
     */
    void startOrStop(Long id, Integer status);
}
