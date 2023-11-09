package com.cvs.service.impl;

import com.cvs.entity.Orders;
import com.cvs.exception.DateException;
import com.cvs.mapper.OrderMapper;
import com.cvs.mapper.UserMapper;
import com.cvs.service.ReportService;
import com.cvs.vo.TurnoverReportVO;
import com.cvs.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        if (begin.isAfter(end)){
            throw new DateException("开始日期不正确");
        }
        //dateList用于存放从begin至end范围内的日期
        ArrayList<LocalDate> dateList = new ArrayList<>();
        ArrayList<BigDecimal> turnoverList = new ArrayList<>();
        dateList.add(begin);
        //这里改为使用mysql中的datediff函数查询，支持查询当天的营业额数据
        BigDecimal turnover = orderMapper.getSumAmountByStatusAndOrderTime(Orders.COMPLETED,begin);
        //防止turnover为空，为空的话传到前端折线图不连续
        turnover = turnover == null ? new BigDecimal(0.0) : turnover;
        turnoverList.add(turnover);
        while (!begin.equals(end)){
            begin = begin.plusDays(1l);
            //在dateList的构建过程中查询出来，比教程少用一次for循环
            turnover = orderMapper.getSumAmountByStatusAndOrderTime(Orders.COMPLETED,begin);
            //防止turnover为空，为空的话传到前端折线图不连续
            turnover = turnover == null ? new BigDecimal(0.0) : turnover;
            dateList.add(begin);
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,','))
                .turnoverList(StringUtils.join(turnoverList,','))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        if (begin.isAfter(end)){
            throw new DateException("开始日期不正确");
        }

        ArrayList<LocalDate> dateList = new ArrayList<>();
        //支持亿级用户数量的精准查询O(∩_∩)O
        List<BigInteger> totalUserList = new ArrayList<>();
        List<BigInteger> newUserList = new ArrayList<>();

        dateList.add(begin);

        BigInteger totalUser = userMapper.getSumUserByDate(begin);
        totalUser = totalUser == null ? new BigInteger(new byte[]{0}) : totalUser;
        totalUserList.add(totalUser);
        newUserList.add(totalUser);

        while (!begin.equals(end)){
            begin = begin.plusDays(1l);
            totalUser = userMapper.getSumUserByDate(begin);
            totalUser = totalUser == null ? new BigInteger(new byte[]{0}) : totalUser;
            dateList.add(begin);
            totalUserList.add(totalUser);
            newUserList.add(totalUser.subtract(totalUserList.get(totalUserList.size() - 1)));
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,','))
                .totalUserList(StringUtils.join(totalUserList,','))
                .newUserList(StringUtils.join(newUserList,','))
                .build();
    }
}
