package com.bakery.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CmsController {

  @GetMapping("/cms/login")
  public String login() {
    return "cms/login";
  }

  @GetMapping("/cms")
  public String dashboard() {
    return "cms/index";
  }
}
