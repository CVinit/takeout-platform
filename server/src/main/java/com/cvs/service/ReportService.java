package com.cvs.service;

import com.cvs.vo.OrderReportVO;
import com.cvs.vo.SalesTop10ReportVO;
import com.cvs.vo.TurnoverReportVO;
import com.cvs.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {
    /**
     * 统计指定时间区间内的营业额数据
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的用户数据
     * @return
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间内的订单数据
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间内的销量top10
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);
}
