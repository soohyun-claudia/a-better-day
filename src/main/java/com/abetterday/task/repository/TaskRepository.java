package com.abetterday.task.repository;

import com.abetterday.task.domain.Task;
import com.abetterday.task.domain.TaskStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** task 엔티티의 리포지토리. */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** 지정한 상태 목록에 해당하는 task를 조회한다. */
    List<Task> findByStatusIn(List<TaskStatus> statuses);

    /** 지정한 상태의 task 첫 건을 조회한다 (DOING 단건 조회용). */
    Optional<Task> findFirstByStatus(TaskStatus status);

    /** 지정한 시간 범위 안에서 완료된 task를 완료 시각 내림차순으로 조회한다. */
    @Query("SELECT t FROM Task t WHERE t.doneAt >= :start AND t.doneAt < :end ORDER BY t.doneAt DESC")
    List<Task> findDoneToday(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
}
