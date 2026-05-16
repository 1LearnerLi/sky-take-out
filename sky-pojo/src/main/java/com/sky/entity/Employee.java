package com.sky.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//自动填从时，若类名与数据库表名不相同，如：若表名为tb_employee时，要加@TableName注解，使类与表对应
//@TableName("tb_employee")
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String name;

    private String password;

    private String phone;

    private String sex;

    private String idNumber;

    private Integer status;

    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    //mybatis-plus自动填充字段功能使用的注解，标记自动填充字段
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    //@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    //mybatis-plus自动填充字段功能使用的注解，标记自动填充字段
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    //同上
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    //同上
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

}
