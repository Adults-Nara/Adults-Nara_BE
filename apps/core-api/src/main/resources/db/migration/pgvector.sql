-- 1. pgvector 확장 활성화 (DB 내에서 한 번만 실행하면 됨)
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. 설치된 확장 리스트 확인
SELECT * FROM pg_extension WHERE extname = 'vector';
