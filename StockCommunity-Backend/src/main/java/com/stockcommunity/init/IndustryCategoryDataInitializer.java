package com.stockcommunity.init;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.stockcommunity.stocks.Industry.IndustryCategory;
import com.stockcommunity.stocks.Industry.IndustryCategoryRepository;

import lombok.RequiredArgsConstructor;

// 啟動完成後第一時間將產業別資料填入產業資料表
@Component
@RequiredArgsConstructor
public class IndustryCategoryDataInitializer implements ApplicationRunner {

	private final IndustryCategoryRepository repository;
	
	@Override
    public void run(ApplicationArguments args) throws Exception {
        // 如果資料庫已經有資料，就不重複寫入
        if (repository.count() > 0) {
            return;
        }

        // 💡 直接把你手動整理的文字，貼在 Java 15+ 的文字區塊 (Text Block) 中
        String rawText = """
                01.水泥工業
				02.食品工業
				03.塑膠工業
				04.紡織纖維
				05.電機機械
				06.電器電纜
				08.玻璃陶瓷
				09.造紙工業
				10.鋼鐵工業
				11.橡膠工業
				12.汽車工業
				13.電子工業
				14.建材營造業
				15.航運業
				16.觀光餐旅
				17.金融保險業
				18.貿易百貨業
				19.綜合
				20.其他業
				21.化學工業
				22.生技醫療業
				23.油電燃氣業
				24.半導體業
				25.電腦及週邊設備業
				26.光電業
				27.通信網路業
				28.電子零組件業
				29.電子通路業
				30.資訊服務業
				31.其他電子業
				32.文化創意業
				33.農業科技業
				35.綠能環保
				36.數位雲端
				37.運動休閒
				38.居家生活
				91.存託憑證
                """;

        List<IndustryCategory> categories = new ArrayList<>();

        // 解析每一行文字
        String[] lines = rawText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || !line.contains(".")) {
                continue;
            }

            // 依據點号 "." 分割代號與名稱
            String[] parts = line.split("\\.", 2);
            String id = parts[0].trim();
            String name = parts[1].trim();

            categories.add(new IndustryCategory(id, name));
        }

        // 批次寫入資料庫，正式生成資料表內容
        repository.saveAll(categories);
        System.out.println("🎉 產業別資料表 (industry_category) 已自動初始化完成！共 " + categories.size() + " 筆。");
    }

}
