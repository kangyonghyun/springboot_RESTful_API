package com.tradlinx.api.article.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ArticleWriteRequest {

    @NotBlank
    private String articleTitle;
    private String articleContents;

}
