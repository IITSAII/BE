# OOM Killer 반복 발생 (2026-09-10, 09-11, 09-14)

## 무슨 일이 있었나

EC2 인스턴스(약 900MB 메모리)에서 java(Spring Boot 앱) 프로세스가 세 차례 반복해서 리눅스 OOM Killer에 강제 종료됨.

```
Out of memory: Killed process 32172 (java) anon-rss:414228kB  (09-10)
Out of memory: Killed process 32843 (java) anon-rss:408068kB  (09-11)
Out of memory: Killed process 55368 (java) anon-rss:490964kB  (09-14)
```

## 원인

- `Dockerfile`의 `ENTRYPOINT`가 `-Xmx` 등 힙 제한 없이 JVM을 기본값으로 실행 → JVM이 시스템 메모리를 계속 확장해서 점유
- 컨테이너에도 `mem_limit`이 없어 상한이 없었음
- `apt-daily.service`, `apt-daily-upgrade.service` 등 APT 자동 업데이트 백그라운드 작업이 순간적으로 메모리를 요청하는 시점에, 이미 JVM이 여유 메모리를 다 써놔서 커널이 가장 메모리를 많이 쓰는 프로세스(java)를 죽임

## 조치 (`8dadca1`, feat/partner-priority-fallback 브랜치)

- `Dockerfile`: `ENTRYPOINT`를 shell form(`sh -c "exec java $JAVA_OPTS -jar app.jar"`)으로 변경해 `JAVA_OPTS` 환경변수 주입 가능하게 함
- `docker-compose-prod.yml`: `mem_limit: 600m` 설정 + `JAVA_OPTS`에 힙·메타스페이스·Direct Memory·Code Cache·스레드 스택 전부 상한 명시 (합산이 600m를 넘지 않도록)
- `NativeMemoryTracking=summary` 상시 활성화 — 재발 시 어느 메모리 영역이 원인인지 dmesg만으로 특정 안 되므로 원인 추적용으로 켜둠

## 확인 방법 (재발 의심 시)

```bash
sudo dmesg -T | grep -i oom
```

`Out of memory: Killed process ... (java)` 라인이 있으면 이 문제가 재발한 것. 배포된 JAVA_OPTS 설정이 실제로 서버에 반영됐는지(`docker compose config` 결과) 먼저 확인할 것.

## 아직 안 한 것 (백로그)

- 스왑 파일 추가 (안전망)
- `apt-daily` 타이머 비활성화 검토
- Serial GC 전환 검토 (힙이 작을 때 G1GC보다 오버헤드 적음)
