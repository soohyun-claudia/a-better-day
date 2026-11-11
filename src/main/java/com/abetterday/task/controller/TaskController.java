package com.abetterday.task.controller;

import com.abetterday.task.domain.TaskStatus;
import com.abetterday.task.dto.ChangeStatusRequest;
import com.abetterday.task.dto.CreateTaskRequest;
import com.abetterday.task.dto.TaskResponse;
import com.abetterday.task.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** task REST API. 요청/응답 변환만 하고 규칙은 서비스에 맡긴다. */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    /** task를 생성하고 201과 Location 헤더로 응답한다. */
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(request);
        return ResponseEntity.created(URI.create("/api/tasks/" + response.id())).body(response);
    }

    /** task 목록을 조회한다. status는 쉼표로 여러 개를 줄 수 있다(예: TODO,DOING). */
    @GetMapping
    public List<TaskResponse> list(@RequestParam(required = false) List<TaskStatus> status) {
        return taskService.getTasks(status);
    }

    /** task의 상태를 바꾼다(체크, ▶, 승격). */
    @PatchMapping("/{id}")
    public TaskResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
        return taskService.changeStatus(id, request.status());
    }
}
