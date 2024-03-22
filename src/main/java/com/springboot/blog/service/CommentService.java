package com.springboot.blog.service;

import com.springboot.blog.payload.dto.CommentDto;
import org.springframework.stereotype.Service;

public interface CommentService {
    CommentDto createComment(Long postId, CommentDto commentDto);
}
