package com.ott.core.modules.search.config;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.core.modules.search.document.VideoDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchOperations elasticsearchOperations;

    @EventListener(ApplicationReadyEvent.class)
    public void initIndices() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(VideoDocument.class);

        // 인덱스가 이미 존재하면 통과 (데이터가 날아가는 것 방지)
        if (indexOps.exists()) {
            log.info("✅ Elasticsearch 인덱스 [video_search]가 이미 존재합니다.");
            return;
        }

        log.info("🚀 Elasticsearch 인덱스 [video_search] 생성을 시작합니다...");

        try {
            // 1. JSON 파일 읽어오기
            String settingsJson = loadJsonFile("elastic/es-settings.json");
            String mappingsJson = loadJsonFile("elastic/es-mappings.json");

            // 2. Settings 설정과 함께 인덱스 생성
            indexOps.create(Document.parse(settingsJson));

            // 3. Mapping 덮어쓰기
            indexOps.putMapping(Document.parse(mappingsJson));

            log.info("🎉 Elasticsearch 인덱스 [video_search]가 커스텀 분석기와 함께 성공적으로 생성되었습니다!");
        } catch (Exception e) {
            log.error("❌ Elasticsearch 인덱스 생성 중 오류 발생. 애플리케이션을 시작할 수 없습니다.", e);

            // 전역 예외 처리 규격인 BusinessException으로 던져서 Fail-Fast 처리
            throw new BusinessException(ErrorCode.ELASTICSEARCH_INIT_ERROR);
        }
    }

    // 리소스 폴더의 파일을 String으로 읽어오는 유틸
    private String loadJsonFile(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path);
                Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            throw new RuntimeException("JSON 설정 파일을 읽을 수 없습니다: " + path, e);
        }
    }
}