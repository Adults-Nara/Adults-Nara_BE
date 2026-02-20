package com.ott.core.modules.search.repository;

import com.ott.core.modules.search.document.VideoDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VideoSearchRepository extends ElasticsearchRepository<VideoDocument, Long> {

}
