package shixipeixun.ranklist.controller;

import shixipeixun.ranklist.dto.RankQueryDTO;
import shixipeixun.ranklist.entity.MerchantRankInfo;
import shixipeixun.ranklist.service.RankService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
public class RankController {

    @Resource
    private RankService rankService;

    @GetMapping("/rank")
    public List<MerchantRankInfo> rank(RankQueryDTO dto) {
        return rankService.getRank(dto);
    }
}