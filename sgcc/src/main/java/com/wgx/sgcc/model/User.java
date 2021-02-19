package com.wgx.sgcc.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "user")
public class User {

    String userName;
    String passWord;
    Date logon;

    public User(String userName, String passWord, Date logon) {
        this.userName = userName;
        this.passWord = passWord;
        this.logon = logon;
    }
}
