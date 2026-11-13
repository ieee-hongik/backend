# 홍익대학교 IEEE 웹사이트 — Backend

홍익대학교 IEEE 웹사이트의 백엔드 저장소입니다.

## 관련 저장소

- Frontend: https://github.com/ieee-hongik/frontend
- Backend: https://github.com/ieee-hongik/backend

## 개발 환경

| 항목 | 버전·도구 |
| --- | --- |
| Java | 21 |
| 초기 실행 확인 JDK | Eclipse Temurin 21.0.12 |
| Spring Boot | 4.1.2 |
| 빌드 도구 | Gradle 9.8.2 · Groovy DSL |
| IDE | IntelliJ IDEA |

Gradle은 저장소에 포함된 Wrapper를 사용합니다.
IntelliJ의 Project SDK와 Gradle JVM을 JDK 21로 설정합니다.

## 현재 구성

- Spring Web MVC
- Validation
- Spring Boot 기본 테스트 환경

기본 프로젝트 실행·테스트·빌드를 확인했습니다.
아직 기능별 API와 데이터베이스 연결은 구성하지 않았습니다.

## 실행

저장소 루트에서 실행합니다.

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

macOS / Linux:

```sh
./gradlew bootRun
```

기본 포트는 8080입니다.
아직 루트 경로의 API가 없으므로 / 요청의 404 응답은 정상입니다.

## 테스트 및 빌드

Windows PowerShell:

```powershell
.\gradlew.bat build
```

macOS / Linux:

```sh
./gradlew build
```

build는 기본 테스트를 포함합니다.
빌드 결과는 build/libs 아래에 생성됩니다.

테스트만 실행할 경우:

```powershell
.\gradlew.bat test
```

## 브랜치 규칙

| 브랜치 | 용도 |
| --- | --- |
| main | 배포용 |
| dev | 개발 통합용 |
| 타입/#이슈번호 | 개별 작업용 |

- 작업 브랜치는 최신 dev에서 생성합니다.
- 작업 브랜치 → dev는 Squash and merge를 사용합니다.
- dev → main은 Create a merge commit을 사용합니다.
- PR 작성자를 제외한 다른 개발자 최소 1명의 승인을 받습니다.
- main 머지는 팀장 DAYOUN1205가 담당합니다.
- main·dev에 직접 푸시하지 않습니다.
- 완료한 작업 브랜치는 수동 삭제합니다.

최초 커밋·브랜치 생성은 보호 설정 적용 전에 진행합니다.

## 브랜치·커밋 타입

| 타입 | 용도 |
| --- | --- |
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| refactor | 기능 변경 없는 코드 구조 개선 |
| docs | 문서 작성·수정 |
| chore | 기타 설정·유지보수 |
| build | 빌드 설정·의존성 변경 |
| test | 테스트 추가·수정 |
| style | 코드 형식 변경 |
| design | UI·CSS 변경 |

브랜치 예: feat/#1

## 커밋 규칙

- 형식: 타입: 변경 내용
- 타입은 영문 소문자, 설명은 한글로 작성합니다.
- 제목 끝에 마침표를 붙이지 않습니다.
- 제목은 가능한 한 50자 이내로 간결하게 작성합니다.
- 커밋에 이슈 번호를 넣지 않습니다.
- 관련 이슈는 작업 브랜치와 PR에 표시합니다.
- 본문은 필요한 경우에만 작성하며 제목과 빈 줄로 구분합니다.

예: chore: 백엔드 기본 프로젝트 및 협업 설정 추가

## 설정 파일 관리

- 공통 설정은 application.properties 등으로 관리합니다.
- 실제 비밀값을 공통 설정 파일에 직접 작성하지 않습니다.
- .env 및 application-local 설정 파일은 Git에서 제외합니다.
- 환경변수 항목이 정해지면 비밀값 없이 예시 파일과 설정 방법을 추가합니다.
- Gradle Wrapper 파일과 gradle-wrapper.jar는 커밋합니다.

## 추후 구성

- 기능명세·ERD에 따른 도메인별 레이어드 구조
- MySQL 연결 및 영속성 계층
- 공통 응답·예외 처리 — 오류 코드 원본 확인 후 구현
- S3·메일·Excel·PDF 관련 설정
- 검사 기준 합의에 따른 GitHub Actions CI

AWS·도메인·HTTPS·배포 파이프라인은 배포 담당자와 별도로 진행합니다.