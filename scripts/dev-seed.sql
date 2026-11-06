-- 개발용 샘플 데이터. 실제 데이터가 있는 DB에서 실행하지 말 것.
-- 다시 넣으려면 DB 초기화(docker compose down -v) 후 실행.
--
-- 실행 (PowerShell):
--   Get-Content scripts/dev-seed.sql -Raw | docker exec -i a_better_day_db psql -U abetterday -d a_better_day
-- 실행 (bash):
--   docker exec -i a_better_day_db psql -U abetterday -d a_better_day < scripts/dev-seed.sql

DO $$
DECLARE
    -- 서울 시간대 기준
    seoul_now   timestamp := now() AT TIME ZONE 'Asia/Seoul';
    seoul_today date      := (now() AT TIME ZONE 'Asia/Seoul')::date;
    -- 이번 주 일요일 (월요일 시작 주, 일요일=0이면 오늘)
    seoul_sun   date      := seoul_today + ((7 - extract(dow from seoul_today)::int) % 7);
    -- 마감 시각 (서울 로컬 timestamp)
    imminent    timestamp := seoul_now + interval '1 hour 30 minutes';  -- 임박 (약 1시간 30분 뒤)
    relaxed     timestamp := seoul_now + interval '4 hours';             -- 여유 있는 시각 마감
    -- created_at 기준 시각 (서울 timestamptz): date + time → timestamp → AT TIME ZONE → timestamptz
    t0  timestamptz := (seoul_today + '09:00'::time) AT TIME ZONE 'Asia/Seoul';            -- 오늘 09:00
    d2  timestamptz := ((seoul_today - 2) + '09:00'::time) AT TIME ZONE 'Asia/Seoul';      -- 2일 전 09:00
    d1  timestamptz := ((seoul_today - 1) + '10:00'::time) AT TIME ZONE 'Asia/Seoul';      -- 1일 전 10:00
    -- DONE done_at (오늘 서울 시간대)
    dn1 timestamptz := (seoul_today + '10:30'::time) AT TIME ZONE 'Asia/Seoul';
    dn2 timestamptz := (seoul_today + '14:00'::time) AT TIME ZONE 'Asia/Seoul';
    dn3 timestamptz := (seoul_today + '16:30'::time) AT TIME ZONE 'Asia/Seoul';
BEGIN
    -- WISH 7개: 학습·운동·사이드 프로젝트, HIGH·MEDIUM·LOW 혼합 (생성: 2일 전 ~ 오늘)
    INSERT INTO task (title, status, priority, category, created_at, updated_at) VALUES
        ('Spring Boot 심화 학습',      'WISH', 'HIGH',   '학습',           d2,                         d2),
        ('JPA N+1 문제 정리',          'WISH', 'MEDIUM', '학습',           d2 + interval '10 minutes',  d2 + interval '10 minutes'),
        ('주 3회 러닝 5km',            'WISH', 'HIGH',   '운동',           d2 + interval '20 minutes',  d2 + interval '20 minutes'),
        ('스트레칭 루틴 만들기',        'WISH', 'MEDIUM', '운동',           d1,                         d1),
        ('푸시업 100개 도전',          'WISH', 'LOW',    '운동',           d1 + interval '10 minutes',  d1 + interval '10 minutes'),
        ('대시보드 사이드 프로젝트',    'WISH', 'HIGH',   '사이드 프로젝트', d1 + interval '20 minutes',  d1 + interval '20 minutes'),
        ('기술 블로그 글쓰기',         'WISH', 'MEDIUM', '사이드 프로젝트', t0 - interval '1 hour',      t0 - interval '1 hour');

    -- TODO 7개: 마감 종류 전부 포함
    --   임박 시각(1.5h) / 여유 시각(4h) / 오늘 중 / 내일 / 이번 주 일요일 / 마감 없음 ×2
    INSERT INTO task (title, status, priority, due_date, due_time, created_at, updated_at) VALUES
        ('코드 리뷰 피드백 반영',  'TODO', 'HIGH',   imminent::date,  imminent::time, t0,                          t0),
        ('기능 개선 PR 올리기',    'TODO', 'MEDIUM', relaxed::date,   relaxed::time,  t0 + interval '5 minutes',    t0 + interval '5 minutes'),
        ('일일 스탠드업 정리',    'TODO', 'HIGH',   seoul_today,     NULL,           t0 + interval '10 minutes',  t0 + interval '10 minutes'),
        ('내일 회의 자료 준비',   'TODO', 'MEDIUM', seoul_today + 1, NULL,           t0 + interval '15 minutes',  t0 + interval '15 minutes'),
        ('주간 보고서 초안 작성',  'TODO', 'LOW',    seoul_sun,       NULL,           t0 + interval '20 minutes',  t0 + interval '20 minutes'),
        ('레퍼런스 문서 읽기',    'TODO', 'MEDIUM', NULL,            NULL,           t0 + interval '25 minutes',  t0 + interval '25 minutes'),
        ('개발 환경 정리',        'TODO', 'LOW',    NULL,            NULL,           t0 + interval '30 minutes',  t0 + interval '30 minutes');

    -- DOING 1개: 상단 바 "지금 집중할 일"
    -- TODO 7 + DOING 1 = 8 → 최대 7 표시, 1개 숨김
    INSERT INTO task (title, status, priority, due_date, due_time, created_at, updated_at) VALUES
        ('API 스펙 문서 작성', 'DOING', 'HIGH', seoul_today, NULL, t0 + interval '35 minutes', t0 + interval '35 minutes');

    -- DONE 3개: 오늘 서울 시간대 완료
    INSERT INTO task (title, status, priority, done_at, created_at, updated_at) VALUES
        ('아침 미팅 준비',     'DONE', 'MEDIUM', dn1, t0 - interval '30 minutes', dn1),
        ('버그 리포트 정리',   'DONE', 'HIGH',   dn2, t0 + interval '2 hours',    dn2),
        ('테스트 케이스 작성', 'DONE', 'MEDIUM', dn3, t0 + interval '4 hours',    dn3);
END $$;
