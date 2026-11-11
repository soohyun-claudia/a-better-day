package com.abetterday.task.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

/** task 엔티티. 논리 삭제(deleted_yn)를 사용하며 모든 조회에서 삭제 행을 자동 제외한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_yn = 'N'")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    private TaskPriority priority;

    private String category;

    private LocalDate dueDate;

    private LocalTime dueTime;

    private OffsetDateTime doneAt;

    @Column(name = "deleted_yn")
    @Convert(converter = DeletedYnConverter.class)
    private boolean deleted;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    private Task(String title, TaskStatus status, TaskPriority priority, String category,
                 LocalDate dueDate, LocalTime dueTime) {
        this.title = title;
        this.status = status;
        this.priority = priority;
        this.category = category;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
    }

    /** 새 task를 만든다. 검증은 호출하는 서비스에서 끝낸 값을 받는다. */
    public static Task create(String title, TaskStatus status, TaskPriority priority, String category,
                              LocalDate dueDate, LocalTime dueTime) {
        return new Task(title, status, priority, category, dueDate, dueTime);
    }

    /** task를 완료(DONE)로 바꾸고 완료 시각을 기록한다. */
    public void markDone(OffsetDateTime now) {
        this.status = TaskStatus.DONE;
        this.doneAt = now;
    }

    /** task를 하는 중(DOING)으로 바꾸고 done_at을 null로 초기화한다. */
    public void markDoing() {
        this.status = TaskStatus.DOING;
        this.doneAt = null;
    }

    /** task를 할 일(TODO)로 바꾸고 done_at을 null로 초기화한다. */
    public void markTodo() {
        this.status = TaskStatus.TODO;
        this.doneAt = null;
    }

    /** WISH에서 TODO로 승격한다. 우선순위는 보통, 마감은 이번 주 일요일로 설정한다. */
    public void promote(LocalDate thisSunday) {
        this.status = TaskStatus.TODO;
        this.priority = TaskPriority.MEDIUM;
        this.dueDate = thisSunday;
        this.dueTime = null;
        this.doneAt = null;
    }
}
