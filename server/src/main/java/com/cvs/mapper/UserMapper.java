package com.cvs.mapper;

import com.cvs.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入用户，返回插入主键值
     * @param user
     */
    void insert(User user);

    /**
     * 根据用户id查询用户
     * @param userId
     * @return
     */
    @Select("select * from user where id = #{userId}")
    User getById(Long userId);

    /**
     * 根据日期查询用户数量
     * @param begin
     * @return
     */
    @Select("select sum(id) from user where datediff(create_time,#{begin}) = 0")
    BigInteger getSumUserByDate(LocalDate begin);

    /**
     * 根据动态条件统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
