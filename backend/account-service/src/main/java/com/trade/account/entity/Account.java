package com.trade.account.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_account")
public class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long userId;              // 用户ID
    private BigDecimal balance;       // 可用余额
    private BigDecimal frozenAmount;  // 冻结金额
    private Integer status;           // 1:正常 0:冻结
    
    @Version
    private Integer version;          // 乐观锁版本
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
