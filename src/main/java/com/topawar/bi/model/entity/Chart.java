package com.topawar.bi.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @TableName chart
 */
@TableName(value ="chart")
@Data
public class Chart implements Serializable {
    private Long id;

    private String goal;

    private String name;

    private String chartData;

    private String chartType;

    private String genChart;

    private String genResult;

    private String status;

    private String execMessage;

    private Long userId;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}