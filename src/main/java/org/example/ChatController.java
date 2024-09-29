package org.example;

import cn.hutool.core.util.IdUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/chat")
public class ChatController {

    Map<String, String> msgMap = new ConcurrentHashMap<>();

    @PostMapping("/send")
    @ResponseBody
    public String send(String msg) {
        String msgId = IdUtil.simpleUUID();
        msgMap.put(msgId, msg);
        return msgId;
    }

    @GetMapping(value = "/conversation/{msgId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter conversation(@PathVariable("msgId") String msgId,
                                   //实现断点续传服务器需要做的
                                   @RequestHeader(value = "last-event-id",required = false)String lastEventId) {
        SseEmitter emitter = new SseEmitter();
        String msg = msgMap.remove(msgId);
        //mock chatgpt response
        new Thread(() -> {
            try {
                for (int i = lastEventId==null?0:Integer.parseInt(lastEventId)+1; i < 10; i++) {
                    ChatMessage  chatMessage =  new ChatMessage("test", new String(i+""));
                    emitter.send(SseEmitter.event().data(chatMessage).id(i+""));
                    Thread.sleep(1000);
                }
                emitter.send(SseEmitter.event().id("10").name("stop").data(""));
                emitter.complete(); // close connetion
                System.out.println(msgId+ "  complete ");
            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        }).start();
        System.out.println(msgId+ "  start");
        return emitter;
    }
}

@Data
@AllArgsConstructor
class ChatMessage {
    String role;
    String content;
}

