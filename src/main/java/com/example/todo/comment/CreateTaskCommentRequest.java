package com.example.todo.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateTaskCommentRequest {

    @NotBlank(message = "Comment content is required")
    @Size(max = 500, message = "Comment must not be greater than 500 characters")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}