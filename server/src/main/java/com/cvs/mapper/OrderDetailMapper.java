package com.cvs.mapper;

import com.cvs.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * 插入数据
     * @param orderDetail
     */
    void insert(OrderDetail orderDetail);

    /**
     * 插入多条数据
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);
}
