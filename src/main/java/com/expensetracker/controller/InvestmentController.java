package com.expensetracker.controller;

import com.expensetracker.model.Investment;
import com.expensetracker.model.InvestmentSummary;
import com.expensetracker.service.InvestmentService;
import com.expensetracker.service.InvestmentStreamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final InvestmentStreamService streamService;

    @Autowired
    public InvestmentController(InvestmentService investmentService, InvestmentStreamService streamService) {
        this.investmentService = investmentService;
        this.streamService = streamService;
    }

    @GetMapping("/stream")
    public SseEmitter streamInvestments() {
        return streamService.createConnection();
    }

    @GetMapping
    public List<Investment> getAllInvestments() {
        return investmentService.getAllInvestments();
    }

    @GetMapping("/summary")
    public InvestmentSummary getSummary() {
        return investmentService.getSummary();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Investment> getInvestmentById(@PathVariable String id) {
        return investmentService.getInvestmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> addInvestment(@RequestBody Investment investment) {
        if (investment.getName() == null || investment.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Investment name is required.");
        }
        if (investment.getType() == null) {
            return ResponseEntity.badRequest().body("Investment type is required.");
        }
        Investment saved = investmentService.addInvestment(investment);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateInvestment(@PathVariable String id, @RequestBody Investment investment) {
        if (investment.getName() == null || investment.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Investment name is required.");
        }
        return investmentService.updateInvestment(id, investment)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvestment(@PathVariable String id) {
        if (investmentService.deleteInvestment(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
