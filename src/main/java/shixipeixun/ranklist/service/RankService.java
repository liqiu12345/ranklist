package shixipeixun.ranklist.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import shixipeixun.ranklist.dto.RankQueryDTO;
import shixipeixun.ranklist.entity.MerchantRankInfo;
import shixipeixun.ranklist.mapper.MerchantRankInfoMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class RankService {

    @Resource
    private MerchantRankInfoMapper merchantRankInfoMapper;

    public List<MerchantRankInfo> getRank(RankQueryDTO dto) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 直接从数据库查询
        List<MerchantRankInfo> allList = merchantRankInfoMapper.selectByDate(date);

        // 根据条件过滤
        List<MerchantRankInfo> result = new ArrayList<>();
        for (MerchantRankInfo item : allList) {
            if (item.getCityId().equals(dto.getCityId())
                    && item.getType().equals(dto.getType())
                    && item.getCategory().equals(dto.getCategory())) {
                result.add(item);
            }
        }
        return result;
    }
}