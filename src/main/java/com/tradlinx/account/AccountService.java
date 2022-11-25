package com.tradlinx.account;

import com.tradlinx.account.form.*;
import com.tradlinx.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtTokenProvider tokenProvider;

    public String processNewAccount(SignUpDto signUpDto) {
        signUpDto.setPw(passwordEncoder.encode(signUpDto.getPw()));
        Account account = accountRepository.save(modelMapper.map(signUpDto, Account.class));
        return account.getUserid();
    }

    public String processLogin(LoginDto loginDto) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getUserid(), loginDto.getPw());
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return tokenProvider.createToken(authentication);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account findAccount = accountRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 ID 입니다"));
        if (!findAccount.getPw().equals(findAccount.getPw())) {
            throw new IllegalArgumentException("잘못된 비밀번호 입니다.");
        }
        return new UserAccount(findAccount);
    }

    public ProfileDto getProfile() {
        return ProfileDto.from(
                getCurrentUserid()
                        .flatMap(accountRepository::findOneWithByUserid)
                        .orElseThrow(() -> new IllegalArgumentException("Member not found"))
        );
    }
    public static Optional<String> getCurrentUserid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            log.info("Security Context에 인증 정보가 없습니다.");
            return Optional.empty();
        }

        String userid = null;
        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
            userid = springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof String) {
            userid = (String) authentication.getPrincipal();
        }
        return Optional.ofNullable(userid);
    }

    public PointsDto getPoints() {
        return PointsDto.from(
                getCurrentUserid()
                        .flatMap(accountRepository::findOneWithByUserid)
                        .orElseThrow(() -> new IllegalArgumentException("Member not found"))
        );
    }
}
