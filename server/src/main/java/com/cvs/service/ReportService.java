package com.cvs.service;

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

}
