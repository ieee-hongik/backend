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
- 자동 작업 실패 기록용 JDBC 저장 계층과 MySQL 드라이버
- Spring Boot 기본 테스트 환경

기능별 API와 관리자 조회 화면은 후속 작업에서 연결합니다.

## 실행

저장소 루트에서 실행합니다.

서버 실행에는 MySQL 데이터베이스가 필요합니다. 실행하는 셸에 `SPRING_DATASOURCE_URL`(예: `jdbc:mysql://localhost:3306/ieee_backend`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`를 설정합니다. 빈 로컬 개발 DB에서는 `SPRING_SQL_INIT_MODE=always`를 설정하면 시작할 때 `src/main/resources/schema.sql`로 실패 기록 테이블을 생성합니다. 운영 DB에 대해서는 배포 담당자가 스키마를 적용합니다.

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

자동 작업 실패 기록 테스트는 임베디드 H2를 사용합니다. MySQL 서버 없이 실행할 수 있습니다.

## 자동 작업에서 실패 기록 남기기

자동 작업 클래스에 `AutomaticJobFailureRecorder`를 주입합니다. 아래 예시는 호출 방식이며 `jobType`과 대상 종류는 업무 코드에서 정한 고정 식별자입니다. 식별자는 영문 대문자로 시작하고 영문 대문자·숫자·밑줄을 최대 80자까지 쓸 수 있습니다. 개별 대상이 없는 작업은 두 번째 인자로 `null`을 전달합니다.

```java
try {
    worker.process(targetId);
} catch (RuntimeException failure) {
    failureRecorder.recordFailure(jobType, new AutomaticJobFailureRecorder.Target(targetType, targetId), failure);
    throw failure;
}
```

위 코드는 스케줄러에서 실제 업무를 수행하는 트랜잭션 메서드를 호출하는 경계에 둡니다. 업무가 실패한 뒤 원래 예외를 다시 던져 스케줄러가 실패를 알 수 있게 합니다. 여러 대상을 처리할 때 중단·계속 여부는 각 업무에서 결정합니다.

`AutomaticJobFailureRecorder`는 `REQUIRES_NEW`로 실패 기록 저장을 별도 트랜잭션에서 처리합니다. 호출 시점에 업무 트랜잭션이 열려 있어도 별도 커밋하므로 업무 롤백과 독립적입니다. 가능하면 업무 트랜잭션을 종료한 뒤 호출하면 동시에 필요한 DB 연결 수를 줄일 수 있습니다.

테이블에는 작업과 대상 식별자, UTC 실패 시각, 예외 클래스, 고정 오류 안내를 저장합니다. 실패 기록 ID는 서버 로그 검색에도 사용합니다. 원본 예외 메시지·스택 트레이스는 이 기능이 저장하거나 로그에 추가하지 않습니다. 기록 저장이 실패하면 DB에 행을 남기지 못할 수 있으며, 로그에 실패 ID·검증된 작업 종류·예외 클래스를 남깁니다. 호출자는 원래 업무 오류를 다시 던질 수 있습니다.

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
- 도메인별 MySQL 테이블 및 영속성 계층 확장
- 공통 응답·예외 처리 — 오류 코드 원본 확인 후 구현
- S3·메일·Excel·PDF 관련 설정
- 검사 기준 합의에 따른 GitHub Actions CI

AWS·도메인·HTTPS·배포 파이프라인은 배포 담당자와 별도로 진행합니다.
