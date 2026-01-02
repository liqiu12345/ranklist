package shixipeixun.ranklist.mapper;

import shixipeixun.ranklist.entity.MerchantRankInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MerchantRankInfoMapper {

    @Select("SELECT id, city_id, type, category, merchant_id, sort, " +
            "       sale_num_month, sale_num_day, date, is_delete, " +
            "       create_time, update_time, " +
            "       rank_cycle, order_cnt, operator " +
            "FROM   merchant_rank_info " +
            "WHERE  date = #{date} AND is_delete = 0")
    List<MerchantRankInfo> selectByDate(String date);
}