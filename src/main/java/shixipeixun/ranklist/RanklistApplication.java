package shixipeixun.ranklist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 启用定时任务
public class RanklistApplication {

    public static void main(String[] args) {
        System.out.println("排行榜服务启动中...");
        System.out.println("功能说明：");
        System.out.println("1. 每天中午12点清空缓存，设置等待状态");
        System.out.println("2. 每隔1分钟检查数据仓库是否有新数据");
        System.out.println("3. 查询时先查Redis，没有再查数据库");
        System.out.println("4. 数据未准备好时返回空列表");

        SpringApplication.run(RanklistApplication.class, args);

        System.out.println("排行榜服务启动完成！");
        System.out.println("访问示例：http://localhost:8080/rank?cityId=1001&type=1&category=2");
    }
}