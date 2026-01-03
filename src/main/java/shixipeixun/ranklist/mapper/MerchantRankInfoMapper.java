package shixipeixun.ranklist.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import shixipeixun.ranklist.entity.MerchantRankInfo;

import java.util.List;

@Mapper
public interface MerchantRankInfoMapper {

    /**
     * 根据日期查询排行榜数据
     * @param date 日期，格式：yyyy-MM-dd
     * @return 排行榜列表
     */
    @Select("SELECT id, city_id, type, category, merchant_id, sort, " +
            "       sale_num_month, sale_num_day, date, is_delete, " +
            "       create_time, update_time, " +
            "       rank_cycle, order_cnt, operator " +
            "FROM merchant_rank_info " +
            "WHERE date = #{date} AND is_delete = 0 " +
            "ORDER BY sort ASC")
    List<MerchantRankInfo> selectByDate(String date);

    /**
     * 根据条件查询排行榜数据
     */
    @Select("SELECT id, city_id, type, category, merchant_id, sort, " +
            "       sale_num_month, sale_num_day, date, is_delete, " +
            "       create_time, update_time, " +
            "       rank_cycle, order_cnt, operator " +
            "FROM merchant_rank_info " +
            "WHERE date = #{date} AND city_id = #{cityId} " +
            "      AND type = #{type} AND category = #{category} " +
            "      AND is_delete = 0 " +
            "ORDER BY sort ASC")
    List<MerchantRankInfo> selectByConditions(String date, String cityId, Integer type, Integer category);
}