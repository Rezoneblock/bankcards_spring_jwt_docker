package com.gordeev.bankcards.controller.admin;

import com.gordeev.bankcards.service.CardBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/block")
@RequiredArgsConstructor
public class AdminCardBlockController {
    private final CardBlockService cardBlockService;

}
