package com.example.todo.common.exception;

public class TaskAlreadyCompletedException extends BadRequestException {

    public TaskAlreadyCompletedException(Long id) {
        super("Task is already completed with id: " + id);
    }
}