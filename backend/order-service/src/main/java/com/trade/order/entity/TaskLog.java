package com.trade.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_task_log")
public class TaskLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskName;       // 任务名称
    private LocalDateTime triggerTime; // 任务触发时间
    private LocalDateTime endTime; // 任务结束时间
    private Integer status;        // 0:失败 1:成功
    private Integer totalCount;    // 扫描/处理总数
    private Integer successCount;  // 成功数量
    private Integer failCount;     // 失败数量
    private String failedNos;      // 失败的业务编号列表
    private String message;        // 执行结果描述

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
