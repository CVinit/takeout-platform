package com.cvs.service.impl;

import com.cvs.entity.Orders;
import com.cvs.mapper.OrderMapper;
import com.cvs.service.ReportService;
import com.cvs.vo.TurnoverReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    /**
     * 统计指定时间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
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
}
