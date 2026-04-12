package com.example.autosalon.dto;

import com.example.autosalon.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTaskResponseDto {
    private long taskId;
    private TaskStatus status;
}