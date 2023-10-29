package com.cvs.service.impl;

import com.cvs.constant.MessageConstant;
import com.cvs.constant.StatusConstant;
import com.cvs.dto.SetmealDTO;
import com.cvs.dto.SetmealPageQueryDTO;
import com.cvs.entity.Category;
import com.cvs.entity.Setmeal;
import com.cvs.entity.SetmealDish;
import com.cvs.exception.DeletionNotAllowedException;
import com.cvs.mapper.CategoryMapper;
import com.cvs.mapper.SetmealDishMapper;
import com.cvs.mapper.SetmealMapper;
import com.cvs.result.PageResult;
import com.cvs.service.SetmealService;
import com.cvs.vo.SetmealVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Override
    public void saveWithDishes(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.insert(setmeal);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> {setmealDish.setSetmealId(setmeal.getId());});
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(),page.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        //起售中的套餐不能删除
        for (Long id:ids){
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //批量删除套餐和套餐-菜品关系表中的数据
        setmealMapper.deleteByIds(ids);
        setmealDishMapper.deleteBySetmealIds(ids);


    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    public void updateWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        List<Long> setmealIds = new ArrayList<>();
        setmealIds.add(setmeal.getId());
        setmealDishMapper.deleteBySetmealIds(setmealIds);
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0){
            setmealDishes.forEach(setmealDish -> {setmealDish.setSetmealId(setmeal.getId());});
            setmealDishMapper.insertBatch(setmealDishes);

        }

    }

    /**
     * 根据id查询套餐及对应菜品
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        Category category = categoryMapper.getById(setmeal.getCategoryId());
        List<SetmealDish> setmealDishes = setmealDishMapper.getSetmealDishBySetmealId(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        setmealVO.setCategoryName(category.getName());
        return setmealVO;
    }

    /**
     * 起售、停售套餐
     * @param id
     * @param status
     */
    @Override
    public void startOrStop(Long id, Integer status) {
        Setmeal setmeal = new Setmeal();
        setmeal.setId(id);
        setmeal.setStatus(status);
        setmealMapper.update(setmeal);
    }
}
