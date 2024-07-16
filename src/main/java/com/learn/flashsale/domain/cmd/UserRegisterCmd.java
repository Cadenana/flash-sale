package com.learn.flashsale.domain.cmd;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserRegisterCmd {

    private String username;

    private String phone;

    private String password;

    private List<Integer> roles;
}
