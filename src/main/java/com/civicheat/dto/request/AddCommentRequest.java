package com.civicheat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddCommentRequest {

    @NotBlank(message = "Comment content cannot be blank")
    @Size(min = 3, max = 2000, message = "Comment must be between 3 and 2000 characters")
    private String content;
}
