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

    /**
     * 获取排行榜接口
     * @param dto 查询参数：cityId, type, category
     * @return 排行榜列表
     */
    @GetMapping("/rank")
    public List<MerchantRankInfo> rank(RankQueryDTO dto) {

        if (dto.getCityId() == null || dto.getType() == null || dto.getCategory() == null) {
            System.out.println("参数错误：cityId=" + dto.getCityId() + ", type=" + dto.getType() + ", category=" + dto.getCategory());
            return List.of(); // 返回空列表
        }

        System.out.println("收到排行榜查询请求：cityId=" + dto.getCityId() + ", type=" + dto.getType() + ", category=" + dto.getCategory());
        return rankService.getRank(dto);
    }
}