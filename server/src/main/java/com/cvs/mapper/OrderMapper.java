package com.cvs.mapper;

import com.cvs.dto.GoodsSalesDTO;
import com.cvs.dto.OrdersPageQueryDTO;
import com.cvs.entity.OrderDetail;
import com.cvs.entity.Orders;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 订单分页查询
     *
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    @Select("select count(id) from orders where status = #{status}")
    Integer countByStatus(Integer status);

    @Select("select * from orders where status = #{status} and order_time < #{time}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime time);

    @Select("select sum(amount) from orders where status = #{status} and datediff(order_time,#{begin}) = 0")
    BigDecimal getSumAmountByStatusAndOrderTime(Integer status, LocalDate begin);

    @Select("select count(id) from orders where datediff(order_time,#{orderDate}) = 0")
    Integer getSumOrderByDate(LocalDate orderDate);

    @Select("select count(id) from  orders where status = #{status} and datediff(order_time,#{orderDate}) = 0")
    Integer getSumValidOrderByDate(Integer status, LocalDate orderDate);

    @Select("select name,sum(number) number from order_detail where order_id in (select id from orders where status = #{status} and order_time > #{begin} and order_time < #{end}) group by name order by number desc limit 0,10")
    List<GoodsSalesDTO> getSalesTop10ByStatusAndDate(Integer status, LocalDateTime begin,LocalDateTime end);

    /**
     * 根据动态条件统计营业额数据
     * @param map
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 根据动态条件统计订单数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
