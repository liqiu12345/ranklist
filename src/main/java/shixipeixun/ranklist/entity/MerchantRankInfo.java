package shixipeixun.ranklist.entity;

import lombok.Data;

@Data
public class MerchantRankInfo {
    private Long id;
    private String cityId;
    private Integer type;
    private Integer category;
    private Long merchantId;
    private Integer sort;
    private Integer saleNumMonth;
    private Integer saleNumDay;
    private String date;
    private Long isDelete;
    private Long createTime;
    private Long updateTime;

    // 新增字段
    private Integer rankCycle;   // 周期类型 1日榜 2周榜 3月榜
    private Integer orderCnt;    // 订单量
    private String operator;     // 操作人
}