package com.tradlinx.article.form;

import lombok.Data;

import java.util.List;

@Data
public class ArticleCommentsDto {
    private String articleId;
    private List<String> commentsId;
}
