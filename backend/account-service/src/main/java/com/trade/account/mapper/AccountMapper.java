package com.trade.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trade.account.entity.Account;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
