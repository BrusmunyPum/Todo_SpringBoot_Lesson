package com.example.todo.user.mapper;

import com.example.todo.user.dto.response.UserResponse;
import com.example.todo.user.entity.AppUser;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class UserMapper {
    public UserResponse toResponse(AppUser user){
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt(), user.getUpdatedAt());
    }
    public List<UserResponse> toResponse(List<AppUser> users){
        return users.stream().map(this::toResponse).toList();
    }
}