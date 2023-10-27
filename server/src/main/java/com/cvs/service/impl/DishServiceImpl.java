package com.cvs.service.impl;

import com.cvs.constant.MessageConstant;
import com.cvs.constant.StatusConstant;
import com.cvs.dto.DishDTO;
import com.cvs.dto.DishPageQueryDTO;
import com.cvs.entity.Dish;
import com.cvs.entity.DishFlavor;
import com.cvs.exception.DeletionNotAllowedException;
import com.cvs.mapper.DishFlavorMapper;
import com.cvs.mapper.DishMapper;
import com.cvs.mapper.SetmealDishMapper;
import com.cvs.result.PageResult;
import com.cvs.service.DishService;
import com.cvs.vo.DishVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
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
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //向菜品表插入一条数据
        dishMapper.insert(dish);
        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors!= null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {dishFlavor.setDishId(dishId);});
            //向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Transactional
    @Override
    public void deleteBatch(List<Long> ids) {
        //当前菜品是否存在起售中的
        for(Long id : ids){
           Dish dish =  dishMapper.getByid(id);
           if (dish.getStatus() == StatusConstant.ENABLE){
               throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
           }
        }
        //当前菜品是否存在被套餐关联的
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //删除菜品表中的菜品数据
//        for (Long id:ids){
//            dishMapper.deleteById(id);
//            dishFlavorMapper.deleteByDishId(id);
//        }
        //删除菜品关联的口味数据

        //批量删除菜品及口味
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);

    }

    /**
     * 根据id查询菜品及口味
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.getByid(id);
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 更新菜品基本信息及口味信息
     * @param dishDTO
     */
    @Transactional
    @Override
    public void updateDishWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        //更新菜品基本信息
        dishMapper.update(dish);
        //删除菜品id对应的口味
        dishFlavorMapper.deleteByDishId(dish.getId());
        //新增菜品口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> {dishFlavor.setDishId(dish.getId());});
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 起售或禁售菜品
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);
        dishMapper.update(dish);
    }
}
