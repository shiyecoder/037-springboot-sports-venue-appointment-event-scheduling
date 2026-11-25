package com.grad.svs.controller;

import com.grad.svs.common.PageResult;
import com.grad.svs.common.R;
import com.grad.svs.entity.SysMessage;
import com.grad.svs.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 站内消息：列表、未读数、标记已读 */
@RestController
@RequestMapping("/api/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/page")
    public R<PageResult<SysMessage>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) Boolean unreadOnly) {
        return R.ok(messageService.page(page, size, unreadOnly));
    }

    @GetMapping("/unread")
    public R<Long> unread() {
        return R.ok(messageService.unreadCount());
    }

    @PostMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        messageService.read(id);
        return R.ok();
    }

    @PostMapping("/read-all")
    public R<Void> readAll() {
        messageService.readAll();
        return R.ok();
    }

    @GetMapping("/latest")
    public R<List<SysMessage>> latest(@RequestParam(defaultValue = "5") int limit) {
        return R.ok(messageService.latest(limit));
    }
}
