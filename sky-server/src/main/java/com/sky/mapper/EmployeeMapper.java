package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
//import com.github.pagehelper.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.Result;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

//@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
    /**
     * 新增员工
     */
    @Insert("insert into employee (name, username, password, phone, sex, id_number, status, create_time, update_time, create_user, update_user) values (#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{status},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    void insertEmployee(Employee employee);


    /**
     * 根据用户名查询员工
     *
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    /**
     * 员工分页查询
     *
     * @param employeePageQueryDTO
     * @return
     */
//    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 启用禁用员工账号或编辑员工信息
     *
     * @param employee
     */
    void updateDynamic(Employee employee);

    /**
     * 根据id查询员工
     *
     * @param id
     * @return
     */
    @Select("select * from employee where id=#{id};")
    Employee getById(long id);

}
