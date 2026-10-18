package com.bakery.account.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cms")
public class AdminAccountController {

  @GetMapping("/accounts")
  public String accounts() {
    return "cms/account/list";
  }

  @GetMapping("/staff")
  public String staff() {
    return "cms/staff/list";
  }
}
