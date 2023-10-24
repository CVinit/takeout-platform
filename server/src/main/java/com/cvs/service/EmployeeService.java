package com.cvs.service;

import com.cvs.dto.EmployeeDTO;
import com.cvs.dto.EmployeeLoginDTO;
import com.cvs.dto.EmployeePageQueryDTO;
import com.cvs.entity.Employee;
import com.cvs.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);
}
