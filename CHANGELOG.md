# Changelog

## [0.7.1](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.7.0...v0.7.1) (2026-02-28)


### Bug Fixes

* 누락된 환경변수 추가 ([df00657](https://github.com/Adults-Nara/Adults-Nara_BE/commit/df00657282d45d9c9d864244876281621fbe0665))

## [0.7.0](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.6.3...v0.7.0) (2026-02-28)


### Features

* S3 삭제 구현 ([a55edfe](https://github.com/Adults-Nara/Adults-Nara_BE/commit/a55edfefbde9f41e0f89d37e3bbf77e9118f2383))
* 광고 API 만들기 ([3e693f0](https://github.com/Adults-Nara/Adults-Nara_BE/commit/3e693f028276bc5b2e8d007cc93bf9d87c6926b2))


### Bug Fixes

* accessToken 시간 수정 ([b53979a](https://github.com/Adults-Nara/Adults-Nara_BE/commit/b53979a31ef9838087f03a5c13024fe68b33e27a))
* application 파일 수정 ([3740cce](https://github.com/Adults-Nara/Adults-Nara_BE/commit/3740cce9b7def65b2b15d1c45f70e0608144fe2a))
* application 파일 오타 수정 ([d945f1e](https://github.com/Adults-Nara/Adults-Nara_BE/commit/d945f1e024414a5ab9c17fdd7ee50b11fad2c511))
* AuthController 타이밍 공격 수정 ([0ed5658](https://github.com/Adults-Nara/Adults-Nara_BE/commit/0ed5658400192eea027fc634677c4f12cbee19c6))
* AuthController 환경변수 수정 ([08d7d10](https://github.com/Adults-Nara/Adults-Nara_BE/commit/08d7d10d2a9528f3ed54a6879eb3ebf1a3945975))
* CORS ([71bc9ee](https://github.com/Adults-Nara/Adults-Nara_BE/commit/71bc9ee740d08292267ae6fc41e56f52d1d050d8))
* ErrorCode IOE httpStatus 500으로 수정 ([f631cc7](https://github.com/Adults-Nara/Adults-Nara_BE/commit/f631cc7928b64981ec743c9c553118678739f488))
* Gemini 지적 사항 수정 ([9a650bd](https://github.com/Adults-Nara/Adults-Nara_BE/commit/9a650bd9fa5e227fe4e88522485c5c358b7b49e5))
* JWT 서멍 키와 ouath state 서명 키 분리 ([b2f71f7](https://github.com/Adults-Nara/Adults-Nara_BE/commit/b2f71f701d676ae724f45fad36fe50d80eb951f7))
* SecurityConfig 세션 관련 수정 및 accessToken 시간 수정 ([9811596](https://github.com/Adults-Nara/Adults-Nara_BE/commit/9811596af61c01090be81a65d75e91ac134e465e))
* SecurityConfig 코드 리뷰 반영 ([092133f](https://github.com/Adults-Nara/Adults-Nara_BE/commit/092133f4ced9c75e3c0aa6c2b59aa96866e22d8f))
* SecurityConfig 특정 유저 시청 통계 조회 로직 추가 ([c9fd764](https://github.com/Adults-Nara/Adults-Nara_BE/commit/c9fd7649c74d62f3e8fd593752274c8ecef40ac2))
* Spring Profiles 환경변수화 ([26be91b](https://github.com/Adults-Nara/Adults-Nara_BE/commit/26be91b0dd7b3768062b64acc91ef573514dda91))
* Swagger API Auth 토큰 입력 칸 추가 ([5ecb39f](https://github.com/Adults-Nara/Adults-Nara_BE/commit/5ecb39ffdfe871f5ddb598ddfffca1a97773d7fe))
* Swagger API server 호출 설정 변경 ([fd90749](https://github.com/Adults-Nara/Adults-Nara_BE/commit/fd90749ef5c3671e2b159323921a776072d2431c))
* VideoService userId 검증 로직 추가 ([1cfc46b](https://github.com/Adults-Nara/Adults-Nara_BE/commit/1cfc46b201729d40d1d22e51cd17fdccfb1e42e2))
* VideoType Long, Short Enum String으로 변경 ([2b9b55d](https://github.com/Adults-Nara/Adults-Nara_BE/commit/2b9b55d3bfde903a42f3ba5458de4396aa810065))
* 광고 API 성능 개선 ([d6750c5](https://github.com/Adults-Nara/Adults-Nara_BE/commit/d6750c522e5d87d1e5b26947fa47dc49ba82bd68))
* 구글 코드 리뷰 피드백 ([68bec1b](https://github.com/Adults-Nara/Adults-Nara_BE/commit/68bec1b9755d40a08ae84a89660d14e1b3899eb3))
* 로그아웃 로직 구현 및 토큰 수정 ([4e7c05e](https://github.com/Adults-Nara/Adults-Nara_BE/commit/4e7c05ee60dc45588ed472256ce85b1daee7342a))
* 시간 UTC 설정으로 고정 ([7b56d4f](https://github.com/Adults-Nara/Adults-Nara_BE/commit/7b56d4f57343f4df7da5ed23dc5c545e966222b0))
* 영상 API 유저 ID 받기 및 업로드 양식 수정 ([1c9d12a](https://github.com/Adults-Nara/Adults-Nara_BE/commit/1c9d12a8a9b105048be16c18348128d47ec6b26f))
* 영상 업로드 API swagger 파일 업로드 설정 ([a303c54](https://github.com/Adults-Nara/Adults-Nara_BE/commit/a303c54f91af12d8e88530869c65bba581cdee8f))
* 영상 트랜스코딩 실패 재시도 로직 수정 및 오류 해결 ([9ad30ee](https://github.com/Adults-Nara/Adults-Nara_BE/commit/9ad30eedf421c3e8bb5bd2d6784e5af2a344b722))
* 영상 트랜스코딩 실패 재시도 로직 추가 ([f5f70c5](https://github.com/Adults-Nara/Adults-Nara_BE/commit/f5f70c5d17a704cc4c04456177864dad001d0a4e))
* 제미나이 피드백 반영 ([b6ccd2b](https://github.com/Adults-Nara/Adults-Nara_BE/commit/b6ccd2b1b958bfcf8bfd1d75a9aefc94d64caeb7))
* 코드 리뷰 수정 ([2876a83](https://github.com/Adults-Nara/Adults-Nara_BE/commit/2876a83d7ac428f669b4a6db51500f930314a7af))
* 코드 리뷰 피드백 반영 ([46f4cc0](https://github.com/Adults-Nara/Adults-Nara_BE/commit/46f4cc0106120b5ed4b29632865c806b0a1c8dec))

## [0.6.3](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.6.2...v0.6.3) (2026-02-24)


### Bug Fixes

* 엔드포인트 변경 및 media-worker 스펙 업(1vcpu, 2GM ) ([20a1909](https://github.com/Adults-Nara/Adults-Nara_BE/commit/20a1909b915132c1c3981ed4b7c2e7633d307247))

## [0.6.2](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.6.1...v0.6.2) (2026-02-23)


### Bug Fixes

* CORS 문제 해결 ([75ff628](https://github.com/Adults-Nara/Adults-Nara_BE/commit/75ff628f388ee93287093c534ea463cb8b73810f))
* CORS 문제 해결 ([737d1db](https://github.com/Adults-Nara/Adults-Nara_BE/commit/737d1dbfd0847a74e7f6e3b3334f75559d9159d5))
* CORS 문제 해결 ([5f9f9f4](https://github.com/Adults-Nara/Adults-Nara_BE/commit/5f9f9f412926208fa0cb5788b70af0a228f4d2d5))

## [0.6.1](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.6.0...v0.6.1) (2026-02-23)


### Bug Fixes

* application-prod.yml 변경사항 메인 브랜치 병합 ([df2331a](https://github.com/Adults-Nara/Adults-Nara_BE/commit/df2331a4d41a9f20a1740532b77d83ccd5310543))

## [0.6.0](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.5.4...v0.6.0) (2026-02-23)


### Features

* OAuth 카카오 로그인 기능 구현 ([d8a3218](https://github.com/Adults-Nara/Adults-Nara_BE/commit/d8a32183d80014797e927479b1e03ccdb32a9af1))
* 백오피스 인증 API 및 유저 프로필 관리 기능 구현 ([e15805f](https://github.com/Adults-Nara/Adults-Nara_BE/commit/e15805f39ffdb8d1c14f1824a837aac38891b712))


### Bug Fixes

* CD.yml 최적화 및 로그인 기능 구현 관련 메인 브랜치 반영 ([3c9e945](https://github.com/Adults-Nara/Adults-Nara_BE/commit/3c9e945132adca93f1ec6a9c8e01efbb536f07b1))
* ddl-auto 수정 ([c2c33c6](https://github.com/Adults-Nara/Adults-Nara_BE/commit/c2c33c655fb89fae962310118ee25dfaf314eea8))
* SecurityConfig 충돌 문제 수정 ([a4a9ec3](https://github.com/Adults-Nara/Adults-Nara_BE/commit/a4a9ec35d2df286a928df81a695f6d21171a9253))
* SecurityConfig에 비디오 관련 로직 수정 ([9ad6306](https://github.com/Adults-Nara/Adults-Nara_BE/commit/9ad6306c7962ec0471bf920dd0ed70cd8cac0836))
* SecurityConfig에 비디오 관련 로직 수정 ([9878657](https://github.com/Adults-Nara/Adults-Nara_BE/commit/9878657258685199cb0208bb51a0f14078e9760d))
* 백오피스 코드 리뷰 수정 ([edf7d11](https://github.com/Adults-Nara/Adults-Nara_BE/commit/edf7d11b41abb4e6a73a9d3bf59085451670ad47))
* 백오피스 코드 리뷰 수정 및 업로더 회원가입 로직 수정 ([d1246e5](https://github.com/Adults-Nara/Adults-Nara_BE/commit/d1246e57fb841566b3d56938002abcddf16608b1))
* 카카오 OAuth 코드 수정 ([a33cf38](https://github.com/Adults-Nara/Adults-Nara_BE/commit/a33cf38739424dbd4ede9ef3b03c724efb820f26))
* 코드 리뷰 봇 수정 ([826c7f4](https://github.com/Adults-Nara/Adults-Nara_BE/commit/826c7f4c5ee00f941573ec38c77d01526533665c))

## [0.5.4](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.5.3...v0.5.4) (2026-02-22)


### Bug Fixes

* CORS 문제 해결 ([b0c80bd](https://github.com/Adults-Nara/Adults-Nara_BE/commit/b0c80bd559b9f03498c2f55a8ec74c66aaeeb1f4))
* CORS 문제 해결 ([2d6d862](https://github.com/Adults-Nara/Adults-Nara_BE/commit/2d6d8621987e68b820148d1841185b073e193000))

## [0.5.3](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.5.2...v0.5.3) (2026-02-22)


### Bug Fixes

* media-worker Dockerfile 수정사항 반영 ([aa66d9a](https://github.com/Adults-Nara/Adults-Nara_BE/commit/aa66d9a13dee1046fde880e706c0f187b77271f0))

## [0.5.2](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.5.1...v0.5.2) (2026-02-21)


### Bug Fixes

* trigger release notes ([ce3d73b](https://github.com/Adults-Nara/Adults-Nara_BE/commit/ce3d73b205c3fa5272bd8911ffa4533cd0e463e4))

## [0.5.1](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.5.0...v0.5.1) (2026-02-21)


### Bug Fixes

* cd에서 발생하는 docker 빌드 문제 해결 ([ea528d5](https://github.com/Adults-Nara/Adults-Nara_BE/commit/ea528d51a592c4a6636a889de96b9b35d87ace5e))

## [0.5.0](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.4.1...v0.5.0) (2026-02-21)


### Features

* .gitignore 수정 ([5301d26](https://github.com/Adults-Nara/Adults-Nara_BE/commit/5301d26a9ee3818ea1bf3b015429e41bc474b08a))

## [0.4.1](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.4.0...v0.4.1) (2026-02-21)


### Bug Fixes

* trigger release ([7813d83](https://github.com/Adults-Nara/Adults-Nara_BE/commit/7813d83b52d40a91f44605fec9c669d132a2f25b))

## [0.4.0](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.3.0...v0.4.0) (2026-02-21)


### Features

* 배포를 위한 main 브랜치로의 병합 ([734dbf7](https://github.com/Adults-Nara/Adults-Nara_BE/commit/734dbf7374a6bd71359bb0dbc0bcd0e530183dff))

## [0.3.0](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.2.1...v0.3.0) (2026-02-21)


### Features

* Ffmpeg 영상 트랜스코딩 작업 구현 ([3d76a55](https://github.com/Adults-Nara/Adults-Nara_BE/commit/3d76a55b7810776800b9a50772b3c7eaa87cb643))
* S3 멀티파트 영상 업로드 기능 구현 ([96ed438](https://github.com/Adults-Nara/Adults-Nara_BE/commit/96ed4383727cb14e54b34bb903a654434bf7ec3b))
* 로그인 엔티티 및 테이블 추가 ([b29700d](https://github.com/Adults-Nara/Adults-Nara_BE/commit/b29700dc439dededb61a19e4ff4cf76205209c13))
* 영상 업로드 구현 ([9447a1e](https://github.com/Adults-Nara/Adults-Nara_BE/commit/9447a1e298861bd478d22b3805e85e2a67268479))
* 영상 업로드시 영상 길이(초) 저장 ([8643fe2](https://github.com/Adults-Nara/Adults-Nara_BE/commit/8643fe286e5f37bafa58abd18cf301173aaf1a7a))
* 영상 재생 작업 구현 ([3758bb2](https://github.com/Adults-Nara/Adults-Nara_BE/commit/3758bb2e68cff214ff5041fa27f5cc69e00abedf))
* 유저 엔티티 및 테이블 구현과 유저 관련 기능 로직 구현 ([663d184](https://github.com/Adults-Nara/Adults-Nara_BE/commit/663d184c9afc8be61b9f3e58f73c21ee37d1c1eb))


### Bug Fixes

* gemini 코드 리뷰 개선 작업 ([afb6f55](https://github.com/Adults-Nara/Adults-Nara_BE/commit/afb6f55e92fa2969d12c211cd875907d6c09c288))
* IOE 코드 500으로 변경 ([370085f](https://github.com/Adults-Nara/Adults-Nara_BE/commit/370085f40389a1e4404bdbf982e59be89e7ce9ec))
* WatchHistory import 빠짐 ([fa6f085](https://github.com/Adults-Nara/Adults-Nara_BE/commit/fa6f08537bf673dd75ee562bafd384b8204476df))
* 유저 엔티티에 BaseEntity 상속, 유저 테이블 수정 ([bb47d88](https://github.com/Adults-Nara/Adults-Nara_BE/commit/bb47d88399155937be4d545d9e8ef0ba011c9275))

## [0.2.0](https://github.com/Adults-Nara/Adults-Nara_BE/compare/v0.1.0...v0.2.0) (2026-02-21)


### Features

* Ffmpeg 영상 트랜스코딩 작업 구현 ([3d76a55](https://github.com/Adults-Nara/Adults-Nara_BE/commit/3d76a55b7810776800b9a50772b3c7eaa87cb643))
* S3 멀티파트 영상 업로드 기능 구현 ([96ed438](https://github.com/Adults-Nara/Adults-Nara_BE/commit/96ed4383727cb14e54b34bb903a654434bf7ec3b))
* 로그인 엔티티 및 테이블 추가 ([b29700d](https://github.com/Adults-Nara/Adults-Nara_BE/commit/b29700dc439dededb61a19e4ff4cf76205209c13))
* 영상 업로드 구현 ([9447a1e](https://github.com/Adults-Nara/Adults-Nara_BE/commit/9447a1e298861bd478d22b3805e85e2a67268479))
* 영상 업로드시 영상 길이(초) 저장 ([8643fe2](https://github.com/Adults-Nara/Adults-Nara_BE/commit/8643fe286e5f37bafa58abd18cf301173aaf1a7a))
* 영상 재생 작업 구현 ([3758bb2](https://github.com/Adults-Nara/Adults-Nara_BE/commit/3758bb2e68cff214ff5041fa27f5cc69e00abedf))
* 유저 엔티티 및 테이블 구현과 유저 관련 기능 로직 구현 ([663d184](https://github.com/Adults-Nara/Adults-Nara_BE/commit/663d184c9afc8be61b9f3e58f73c21ee37d1c1eb))


### Bug Fixes

* gemini 코드 리뷰 개선 작업 ([afb6f55](https://github.com/Adults-Nara/Adults-Nara_BE/commit/afb6f55e92fa2969d12c211cd875907d6c09c288))
* IOE 코드 500으로 변경 ([370085f](https://github.com/Adults-Nara/Adults-Nara_BE/commit/370085f40389a1e4404bdbf982e59be89e7ce9ec))
* WatchHistory import 빠짐 ([fa6f085](https://github.com/Adults-Nara/Adults-Nara_BE/commit/fa6f08537bf673dd75ee562bafd384b8204476df))
* 유저 엔티티에 BaseEntity 상속, 유저 테이블 수정 ([bb47d88](https://github.com/Adults-Nara/Adults-Nara_BE/commit/bb47d88399155937be4d545d9e8ef0ba011c9275))
