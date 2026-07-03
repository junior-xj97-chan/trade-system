package com.trade.quote.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.trade.common.R;
import com.trade.quote.dto.WatchlistDTO;
import com.trade.quote.entity.Watchlist;
import com.trade.quote.feign.ProductFeignClient;
import com.trade.quote.feign.UserFeignClient;
import com.trade.quote.mapper.WatchlistMapper;
import com.trade.quote.service.WatchlistService;
import com.trade.quote.vo.WatchlistVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl extends ServiceImpl<WatchlistMapper, Watchlist> implements WatchlistService {

    private final UserFeignClient userFeignClient;
    private final ProductFeignClient productFeignClient;

    @Override
    public List<WatchlistVO> listByUserId(Long userId) {
        R<Boolean> userCheck = userFeignClient.checkUserExists(userId);
        if (userCheck == null || !userCheck.isSuccess() || !Boolean.TRUE.equals(userCheck.getData())) {
            throw new RuntimeException("User not found: " + userId);
        }

        LambdaQueryWrapper<Watchlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Watchlist::getUserId, userId)
                .orderByDesc(Watchlist::getCreateTime);

        List<Watchlist> list = list(wrapper);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Long add(Long userId, WatchlistDTO dto) {
        LambdaQueryWrapper<Watchlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Watchlist::getUserId, userId)
                .eq(Watchlist::getStockCode, dto.getStockCode())
                .eq(Watchlist::getMarket, dto.getMarket());
        if (count(wrapper) > 0) {
            throw new RuntimeException("Stock already in watchlist");
        }

        Watchlist watchlist = new Watchlist();
        watchlist.setUserId(userId);
        watchlist.setStockCode(dto.getStockCode());
        watchlist.setStockName(dto.getStockName());
        watchlist.setMarket(dto.getMarket());
        watchlist.setTags(dto.getTags());
        watchlist.setNote(dto.getNote());
        save(watchlist);
        log.info("Added watchlist: userId={}, stockCode={}", userId, dto.getStockCode());
        return watchlist.getId();
    }

    @Override
    public void remove(Long userId, Long id) {
        LambdaQueryWrapper<Watchlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Watchlist::getId, id).eq(Watchlist::getUserId, userId);
        if (count(wrapper) == 0) {
            throw new RuntimeException("Watchlist not found");
        }
        removeById(id);
        log.info("Removed watchlist: id={}", id);
    }

    @Override
    public void updateTags(Long userId, Long id, String tags) {
        LambdaQueryWrapper<Watchlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Watchlist::getId, id).eq(Watchlist::getUserId, userId);
        if (count(wrapper) == 0) {
            throw new RuntimeException("Watchlist not found");
        }
        Watchlist watchlist = new Watchlist();
        watchlist.setId(id);
        watchlist.setTags(tags);
        updateById(watchlist);
    }

    @Override
    public void updateNote(Long userId, Long id, String note) {
        LambdaQueryWrapper<Watchlist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Watchlist::getId, id).eq(Watchlist::getUserId, userId);
        if (count(wrapper) == 0) {
            throw new RuntimeException("Watchlist not found");
        }
        Watchlist watchlist = new Watchlist();
        watchlist.setId(id);
        watchlist.setNote(note);
        updateById(watchlist);
    }

    private WatchlistVO toVO(Watchlist entity) {
        WatchlistVO vo = new WatchlistVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
