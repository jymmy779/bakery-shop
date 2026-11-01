package com.bakery.dashboard.controller.admin;

import com.bakery.dashboard.service.CmsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class CmsDashboardController {

  private final CmsDashboardService dashboardService;

  @GetMapping("/cms")
  public String dashboard(Model model) {
    model.addAttribute("dashboard", dashboardService.getSummary());
    return "cms/index";
  }
}
