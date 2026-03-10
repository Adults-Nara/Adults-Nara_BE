package com.ott.core.modules.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentEditRequest {

    @NotBlank
    @Size(max = 500)
    private String text;
}
