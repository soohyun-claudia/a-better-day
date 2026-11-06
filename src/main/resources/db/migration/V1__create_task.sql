-- task 테이블 생성
-- 마감은 due_date(DATE) + due_time(TIME) 두 컬럼으로 저장한다.
-- 삭제는 논리 삭제(deleted_yn)다.

CREATE TABLE task (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(200)    NOT NULL,
    status      VARCHAR(10)     NOT NULL,
    priority    VARCHAR(10)     NOT NULL DEFAULT 'MEDIUM',
    category    VARCHAR(50),
    due_date    DATE,
    due_time    TIME,
    done_at     TIMESTAMPTZ,
    deleted_yn  CHAR(1)         NOT NULL DEFAULT 'N',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_task_status      CHECK (status     IN ('WISH', 'TODO', 'DOING', 'DONE')),
    CONSTRAINT chk_task_priority    CHECK (priority   IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_task_deleted_yn  CHECK (deleted_yn IN ('Y', 'N')),

    -- due_time은 due_date가 있을 때만 존재할 수 있다 (시각만 단독 저장 금지)
    CONSTRAINT chk_task_due          CHECK (due_time IS NULL OR due_date IS NOT NULL),

    -- DONE이면 done_at이 있어야 하고, DONE이 아니면 done_at이 없어야 한다
    CONSTRAINT chk_task_done_at      CHECK ((status = 'DONE') = (done_at IS NOT NULL))
);

-- DOING은 삭제되지 않은 행 중 동시에 1개만 허용
CREATE UNIQUE INDEX uq_task_single_doing
    ON task (status)
    WHERE status = 'DOING' AND deleted_yn = 'N';

-- 조회 패턴용 인덱스
CREATE INDEX idx_task_status
    ON task (status);

CREATE INDEX idx_task_done_at
    ON task (done_at)
    WHERE done_at IS NOT NULL AND deleted_yn = 'N';
