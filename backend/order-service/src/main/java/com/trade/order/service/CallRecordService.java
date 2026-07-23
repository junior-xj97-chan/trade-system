package com.trade.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.trade.order.entity.CallRecord;
import com.trade.order.mapper.CallRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallRecordService {

    private final CallRecordMapper callRecordMapper;

    /**
     * 初始化调用记录（幂等：同一业务同一接口只初始化一次）
     */
    @Transactional(rollbackFor = Exception.class)
    public CallRecord init(String bizNo, String bizType, String targetService, String targetMethod,
                           String requestParam, int maxRetry) {
        LambdaQueryWrapper<CallRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CallRecord::getBizNo, bizNo)
               .eq(CallRecord::getTargetService, targetService)
               .eq(CallRecord::getTargetMethod, targetMethod);
        CallRecord record = callRecordMapper.selectOne(wrapper);
        if (record != null) {
            return record;
        }

        record = new CallRecord();
        record.setBizNo(bizNo);
        record.setBizType(bizType);
        record.setTargetService(targetService);
        record.setTargetMethod(targetMethod);
        record.setRequestParam(requestParam);
        record.setStatus(0);
        record.setRetryCount(0);
        record.setMaxRetry(maxRetry);
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        callRecordMapper.insert(record);
        return record;
    }

    /**
     * 标记为处理中
     */
    @Transactional(rollbackFor = Exception.class)
    public void markProcessing(Long id) {
        CallRecord record = new CallRecord();
        record.setId(id);
        record.setStatus(1);
        record.setUpdateTime(LocalDateTime.now());
        callRecordMapper.updateById(record);
    }

    /**
     * 标记为成功
     */
    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(Long id, String response) {
        CallRecord record = new CallRecord();
        record.setId(id);
        record.setStatus(2);
        record.setResponse(response);
        record.setErrorMsg(null);
        record.setUpdateTime(LocalDateTime.now());
        callRecordMapper.updateById(record);
    }

    /**
     * 标记为失败
     */
    @Transactional(rollbackFor = Exception.class)
    public void markFail(Long id, String errorMsg) {
        CallRecord record = callRecordMapper.selectById(id);
        if (record == null) {
            return;
        }
        int newRetry = record.getRetryCount() + 1;
        LambdaUpdateWrapper<CallRecord> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CallRecord::getId, id)
               .set(CallRecord::getStatus, newRetry >= record.getMaxRetry() ? 4 : 3)
               .set(CallRecord::getRetryCount, newRetry)
               .set(CallRecord::getErrorMsg, errorMsg)
               .set(CallRecord::getUpdateTime, LocalDateTime.now());
        callRecordMapper.update(null, wrapper);
    }

    /**
     * 查询需要补偿重试的调用记录
     */
    public List<CallRecord> findRetryRecords(int limit) {
        LambdaQueryWrapper<CallRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CallRecord::getStatus, 3)
               .apply("retry_count < max_retry")
               .orderByAsc(CallRecord::getUpdateTime)
               .last("LIMIT " + limit);
        return callRecordMapper.selectList(wrapper);
    }

    /**
     * 查询指定业务的所有调用记录
     */
    public List<CallRecord> findByBizNo(String bizNo) {
        LambdaQueryWrapper<CallRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CallRecord::getBizNo, bizNo)
               .orderByAsc(CallRecord::getCreateTime);
        return callRecordMapper.selectList(wrapper);
    }
}
