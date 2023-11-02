package com.cvs.controller.user;

import com.cvs.constant.StatusConstant;
import com.cvs.entity.Dish;
import com.cvs.result.Result;
import com.cvs.service.DishService;
import com.cvs.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        String key = "dish_" + categoryId;

        //从redis中查询key对应的value
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);

        if (list != null && list.size() > 0){
            //redis从存在对应value，直接返回
            return Result.success(list);
        }
        //不存在对应value，从数据库中查询
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        list = dishService.listWithFlavor(dish);
        //存储到redis中
        redisTemplate.opsForValue().set(key,list);

        return Result.success(list);
    }

}
