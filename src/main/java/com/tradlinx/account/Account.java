package com.tradlinx.account;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Account {

    @Id
    String userid;
    String pw;
    String username;

    public Account(String userid, String pw) {
        this.userid = userid;
        this.pw = pw;
    }
}
