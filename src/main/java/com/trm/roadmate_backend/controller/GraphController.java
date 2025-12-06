package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.service.GraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    // 💡 그래프를 메모리로 다시 로드하는 API
    @PostMapping("/reload")
    public String reloadGraphData() {
        graphService.loadGraphData(); // loadGraphData 메서드 호출
        return "Graph data reloaded successfully in memory.";
    }
}