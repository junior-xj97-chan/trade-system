package com.trade.quote.service;

import com.trade.quote.dto.WatchlistDTO;
import com.trade.quote.vo.WatchlistVO;

import java.util.List;

public interface WatchlistService {

    List<WatchlistVO> listByUserId(Long userId);

    Long add(Long userId, WatchlistDTO dto);

    void remove(Long userId, Long id);

    void updateTags(Long userId, Long id, String tags);

    void updateNote(Long userId, Long id, String note);
}
