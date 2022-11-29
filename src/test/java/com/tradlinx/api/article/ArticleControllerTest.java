package com.tradlinx.api.article;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradlinx.api.account.Account;
import com.tradlinx.api.account.AccountRepository;
import com.tradlinx.api.account.AccountService;
import com.tradlinx.api.account.form.LoginDto;
import com.tradlinx.api.article.Article;
import com.tradlinx.api.article.ArticleRepository;
import com.tradlinx.api.article.ArticleService;
import com.tradlinx.api.article.form.ArticleUpdateDto;
import com.tradlinx.api.account.form.SignUpDto;
import com.tradlinx.api.article.form.ArticleDto;
import com.tradlinx.api.article.form.CommentDto;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    AccountService accountService;
    @Autowired
    ArticleRepository articleRepository;
    @Autowired
    ArticleService articleService;

    SignUpDto signUpDto;
    ArticleDto articleDto;

    @BeforeEach
    void beforeEach() {
        signUpDto = new SignUpDto();
        signUpDto.setUserid("userid");
        signUpDto.setPw("passw0rd");
        signUpDto.setUsername("username");
        accountService.processNewAccount(signUpDto);

        articleDto = new ArticleDto();
        articleDto.setArticleTitle("articleTitle");
        articleDto.setArticleContents("articleContents");
    }

    @AfterEach
    void afterEach() {
//        accountRepository.deleteAll();
//        articleRepository.deleteAll();
    }

    @WithUserDetails(value = "userid", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @Test
    @DisplayName("글 작성 - 포인트 증가 +3")
    void article_write_success() throws Exception {
        mockMvc.perform(post("/article")
                        .header(HttpHeaders.AUTHORIZATION, new StringStartsWith("Bearer "))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(articleDto)))
                .andExpect(status().isOk())
                .andExpect(authenticated())
                .andExpect(jsonPath("articleId", is("articleId")));

        Article article = articleRepository.findById("articleId").orElseThrow();
        assertThat(article.getArticleTitle()).isEqualTo("articleTitle");
        assertThat(article.getArticleContents()).isEqualTo("articleContents");

        Account account = accountRepository.findById("userid").orElseThrow();
        assertThat(account.getPoints()).isEqualTo(3);
    }

    @WithUserDetails(value = "userid", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @Test
    @DisplayName("글 수정")
    void article_update_success() throws Exception {
        articleService.writeArticle(articleDto);

        ArticleUpdateDto articleUpdateDto = new ArticleUpdateDto();
        articleUpdateDto.setArticleId("articleId");
        articleUpdateDto.setArticleTitle("updateArticleTitle");
        articleUpdateDto.setArticleContents("updateArticleContents");

        mockMvc.perform(put("/article")
                        .header(HttpHeaders.AUTHORIZATION, new StringStartsWith("Bearer "))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(articleUpdateDto)))
                .andExpect(status().isOk())
                .andExpect(authenticated())
                .andExpect(jsonPath("articleId", is("articleId")));

        Article article = articleRepository.findById("articleId").orElseThrow();
        assertThat(article.getArticleTitle()).isEqualTo("updateArticleTitle");
        assertThat(article.getArticleContents()).isEqualTo("updateArticleContents");
    }

    @WithUserDetails(value = "userid", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @Test
    @DisplayName("글 삭제 - 포인트 감소 -3")
    void article_delete_success() throws Exception {
        articleService.writeArticle(articleDto);

        Account account = accountRepository.findById("userid").orElseThrow();
        assertThat(account.getPoints()).isEqualTo(3);

        mockMvc.perform(delete("/article/articleId")
                        .header(HttpHeaders.AUTHORIZATION, new StringStartsWith("Bearer ")))
                .andExpect(status().isOk())
                .andExpect(authenticated())
                .andExpect(jsonPath("count", is(1)));

        assertThat(articleRepository.findById("articleId")).isEmpty();
        assertThat(account.getPoints()).isEqualTo(0);
    }

    @WithUserDetails(value = "userid", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @Test
    @DisplayName("글 조회 - commentId 응답")
    void article_get_success() throws Exception {
        articleService.writeArticle(articleDto);

        mockMvc.perform(get("/article/articleId")
                        .header(HttpHeaders.AUTHORIZATION, new StringStartsWith("Bearer ")))
                .andExpect(status().isOk())
                .andExpect(authenticated())
                .andExpect(jsonPath("articleId", is("articleId")))
                .andExpect(jsonPath("commentsId", is(new ArrayList())));
    }

    @WithUserDetails(value = "userid", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @Test
    @DisplayName("댓글 작성 - commentId 응답, 댓글 작성자 points +2, 원글 작성자 points +1")
    void comment_write_success() throws Exception {
        articleService.writeArticle(articleDto);

        anotherAccountAndLogin();

        CommentDto commentDto = new CommentDto();
        commentDto.setArticleId("articleId");
        commentDto.setCommentContents("commentsContents");

        mockMvc.perform(post("/comments")
                        .header(HttpHeaders.AUTHORIZATION, new StringStartsWith("Bearer "))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isOk())
                .andExpect(authenticated())
                .andExpect(jsonPath("commentId", is("commentId")));

        Article article = articleRepository.findById("articleId").orElseThrow();
        assertThat(article.getAccount().getPoints()).isEqualTo(4);

        Account account = accountRepository.findById("userid2").orElseThrow();
        assertThat(account.getPoints()).isEqualTo(2);
    }

    @WithUserDetails(value = "userid", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @Test
    @DisplayName("댓글 삭제 - commentId 응답, 댓글 작성자 points -2, 원글 작성자 points -1")
    void comment_delete_success() throws Exception {
        articleService.writeArticle(articleDto);

        anotherAccountAndLogin();

        CommentDto commentDto = new CommentDto();
        commentDto.setArticleId("articleId");
        commentDto.setCommentContents("commentsContents");
        articleService.writeComment(commentDto);

        mockMvc.perform(delete("/comments/commentId")
                        .header(HttpHeaders.AUTHORIZATION, new StringStartsWith("Bearer ")))
                .andExpect(status().isOk())
                .andExpect(authenticated())
                .andExpect(jsonPath("commentId", is("commentId")));

        Article article = articleRepository.findById("articleId").orElseThrow();
        assertThat(article.getAccount().getPoints()).isEqualTo(3);

        Account account = accountRepository.findById("userid2").orElseThrow();
        assertThat(account.getPoints()).isEqualTo(0);
    }

    private void anotherAccountAndLogin() {
        SignUpDto signUpDto1 = new SignUpDto();
        signUpDto1.setUserid("userid2");
        signUpDto1.setPw("passw0rd");
        signUpDto1.setUsername("username");
        accountService.processNewAccount(signUpDto1);
        LoginDto loginDto = new LoginDto();
        loginDto.setUserid("userid2");
        loginDto.setPw("passw0rd");
        accountService.processLogin(loginDto);
    }

}