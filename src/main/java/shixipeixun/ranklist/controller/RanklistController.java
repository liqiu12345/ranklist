package shixipeixun.ranklist.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import shixipeixun.ranklist.dto.RankQueryDTO;
import shixipeixun.ranklist.entity.MerchantRankInfo;
import shixipeixun.ranklist.service.RankService;

import java.util.List;

@RestController
public class RanklistController {

    @Resource
    private RankService rankService;

    @GetMapping("/rank")
    public List<MerchantRankInfo> rank(RankQueryDTO dto) {
        return rankService.getRank(dto);
    }
}