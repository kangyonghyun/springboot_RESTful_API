package com.tradlinx.api.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradlinx.api.account.Account;
import com.tradlinx.api.account.AccountRepository;
import com.tradlinx.api.account.AccountService;
import com.tradlinx.api.account.form.LoginDto;
import com.tradlinx.api.account.form.SignUpDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    AccountService accountService;

    SignUpDto signUpDto;
    @BeforeEach
    void beforeEach() {
        signUpDto = new SignUpDto();
        signUpDto.setUserid("userid");
        signUpDto.setPw("passw0rd");
        signUpDto.setUsername("username");
        accountService.processNewAccount(signUpDto);
    }

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("회원 가입 - 성공")
    void signUp_correct_input() throws Exception {
        mockMvc.perform(post("/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpDto)))
                .andExpect(status().isOk())
                .andExpect(unauthenticated());

        Account newAccount = accountRepository.findById("userid").orElseThrow();
        assertThat(newAccount.getUserid()).isEqualTo("userid");
        assertThat(newAccount.getPw()).isNotEqualTo("passw0rd");
    }

    @WithUserDetails(value = "userid", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @Test
    @DisplayName("로그인 - 성공")
    void login_success() throws Exception {
        LoginDto loginDto = new LoginDto();
        loginDto.setUserid("userid");
        loginDto.setPw("passw0rd");

        mockMvc.perform(post("/signin")
                        .content(objectMapper.writeValueAsString(loginDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, new StringStartsWith("Bearer ")))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("userid"))
                .andDo(print());
    }

    @WithUserDetails(value = "userid", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @Test
    @DisplayName("프로필 조회")
    void get_profile() throws Exception {
        mockMvc.perform(get("/profile")
                        .header(HttpHeaders.AUTHORIZATION, new StringStartsWith("Bearer ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("userid", is("userid")))
                .andExpect(jsonPath("username", is("username")))
                .andExpect(authenticated().withUsername("userid"));
    }

    @WithUserDetails(value = "userid", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    @Test
    @DisplayName("포인트 조회")
    void get_points() throws Exception {
        mockMvc.perform(get("/points")
                        .header(HttpHeaders.AUTHORIZATION, new StringStartsWith("Bearer ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("points", is(0)))
                .andExpect(authenticated().withUsername("userid"));
    }

}