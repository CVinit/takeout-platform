package com.cvs.service;

import com.cvs.dto.DishDTO;

public interface DishService {

    /**
     * 新增菜品和对应的口味数据
     * @param dishDTO
     */
    public void saveWithFlavor(DishDTO dishDTO);
}
