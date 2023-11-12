package com.cvs.service.impl;

import com.cvs.dto.GoodsSalesDTO;
import com.cvs.entity.OrderDetail;
import com.cvs.entity.Orders;
import com.cvs.exception.DateException;
import com.cvs.mapper.OrderMapper;
import com.cvs.mapper.UserMapper;
import com.cvs.service.ReportService;
import com.cvs.service.WorkspaceService;
import com.cvs.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

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
        List<LocalDate> dateList = new ArrayList<>();
        List<BigDecimal> turnoverList = new ArrayList<>();
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

        List<LocalDate> dateList = new ArrayList<>();
        //这里应该用Long
        //练习BigInteger的使用
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

    /**
     * 根据指定时间统计订单数据
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        if (begin.isAfter(end)){
            throw new DateException("开始日期不正确");
        }

        List<LocalDate> dateList = new ArrayList<>();
        List<Integer> totalOrderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        dateList.add(begin);

        Integer orderCount = orderMapper.getSumOrderByDate(begin);
        Integer validOrderCount = orderMapper.getSumValidOrderByDate(Orders.COMPLETED, begin);
        totalOrderCountList.add(orderCount);
        validOrderCountList.add(validOrderCount);

        Integer totalOrderCount = orderCount;
        Integer totalValidCount = validOrderCount;

        while (!begin.equals(end)){
            begin = begin.plusDays(1l);
            orderCount = orderMapper.getSumOrderByDate(begin);
            validOrderCount = orderMapper.getSumValidOrderByDate(Orders.COMPLETED, begin);

            dateList.add(begin);
            totalOrderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);

            totalOrderCount += orderCount;
            totalValidCount += validOrderCount;

        }

        Double orderCompletionRate = 0.0;
        if (!totalOrderCount.equals(0)){
            orderCompletionRate =  totalValidCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,','))
                .orderCountList(StringUtils.join(totalOrderCountList,','))
                .validOrderCountList(StringUtils.join(validOrderCountList,','))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(totalValidCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间内的销量top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        if (begin.isAfter(end)){
            throw new DateException("开始日期不正确");
        }

        List<GoodsSalesDTO> top10 = orderMapper.getSalesTop10ByStatusAndDate(Orders.COMPLETED, LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));

        List<String> nameList = top10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = top10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());


        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList,','))
                .numberList(StringUtils.join(numberList,','))
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFCellStyle cellStyle = excel.createCellStyle();
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            XSSFCell cell = sheet.getRow(1).getCell(1);
            cell.setCellStyle(cellStyle);
            cell.setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //3. 通过输出流将Excel文件下载到客户端浏览器

    }
}
