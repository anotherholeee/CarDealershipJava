package com.example.autosalon.dto;

import com.example.autosalon.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AsyncTaskResponse {
    private long id;
    private TaskStatus status;
}
