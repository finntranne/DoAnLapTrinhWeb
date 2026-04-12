package com.alotra.controller.admin;

import com.alotra.service.cloudinary.strategy.UploadService;
import com.alotra.service.cloudinary.strategy.UploadStrategyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Upload Configuration Controller
 * Allows admin to manage upload strategies and view statistics
 */
@Controller
@RequestMapping("/admin/upload-config")
@RequiredArgsConstructor
@Slf4j
public class UploadConfigController {

    private final UploadStrategyContext strategyContext;
    private final UploadService uploadService;

    /**
     * Display upload configuration page
     */
    @GetMapping
    public String showUploadConfig(Model model) {
        String currentStrategy = strategyContext.getCurrentStrategyName();
        Map<String, Object> statistics = strategyContext.getUploadStatistics();
        
        model.addAttribute("activeMenu", "upload-config");
        model.addAttribute("currentStrategy", currentStrategy);
        model.addAttribute("availableStrategies", Arrays.asList(
                "CLOUDINARY_WITH_PROXY",
                "CLOUDINARY", 
                "LOCAL_STORAGE"
        ));
        model.addAttribute("statistics", statistics);
        model.addAttribute("uploadHistory", strategyContext.getUploadHistory());
        
        log.info("Admin accessed upload configuration page. Current strategy: {}", currentStrategy);
        
        return "admin/upload-config";
    }

    /**
     * Change upload strategy via web form
     */
    @PostMapping("/change-strategy")
    public String changeStrategy(@RequestParam("strategy") String strategyName, Model model) {
        try {
            log.info("Admin requested strategy change to: {}", strategyName);
            
            strategyContext.setStrategyByName(strategyName);
            
            model.addAttribute("activeMenu", "upload-config");
            model.addAttribute("successMessage", 
                    "✅ Đã thay đổi chiến lược upload thành: " + strategyName);
            model.addAttribute("currentStrategy", strategyContext.getCurrentStrategyName());
            model.addAttribute("statistics", strategyContext.getUploadStatistics());
            model.addAttribute("availableStrategies", Arrays.asList(
                    "CLOUDINARY_WITH_PROXY",
                    "CLOUDINARY",
                    "LOCAL_STORAGE"
            ));
            model.addAttribute("uploadHistory", strategyContext.getUploadHistory());
            
            return "admin/upload-config";
        } catch (Exception e) {
            log.error("Error changing upload strategy: {}", e.getMessage());
            model.addAttribute("activeMenu", "upload-config");
            model.addAttribute("errorMessage", "❌ Lỗi: " + e.getMessage());
            model.addAttribute("currentStrategy", strategyContext.getCurrentStrategyName());
            model.addAttribute("availableStrategies", Arrays.asList(
                    "CLOUDINARY_WITH_PROXY",
                    "CLOUDINARY",
                    "LOCAL_STORAGE"
            ));
            model.addAttribute("statistics", strategyContext.getUploadStatistics());
            model.addAttribute("uploadHistory", strategyContext.getUploadHistory());
            return "admin/upload-config";
        }
    }

    /**
     * REST API: Get current strategy info
     */
    @GetMapping("/api/current-strategy")
    @ResponseBody
    public Map<String, Object> getCurrentStrategy() {
        Map<String, Object> response = new HashMap<>();
        response.put("currentStrategy", strategyContext.getCurrentStrategyName());
        response.put("availableStrategies", Arrays.asList(
                "CLOUDINARY_WITH_PROXY",
                "CLOUDINARY",
                "LOCAL_STORAGE"
        ));
        response.put("statistics", strategyContext.getUploadStatistics());
        return response;
    }

    /**
     * REST API: Change strategy
     */
    @PostMapping("/api/set-strategy")
    @ResponseBody
    public Map<String, Object> setStrategy(@RequestParam("strategy") String strategyName) {
        Map<String, Object> response = new HashMap<>();
        try {
            strategyContext.setStrategyByName(strategyName);
            response.put("success", true);
            response.put("message", "Strategy changed to: " + strategyName);
            response.put("currentStrategy", strategyContext.getCurrentStrategyName());
            log.info("Strategy changed by admin to: {}", strategyName);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            log.error("Failed to change strategy: {}", e.getMessage());
        }
        return response;
    }

    /**
     * REST API: Get statistics
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public Map<String, Object> getStatistics() {
        return strategyContext.getUploadStatistics();
    }

    /**
     * REST API: Clear upload cache
     */
    @PostMapping("/api/clear-cache")
    @ResponseBody
    public Map<String, Object> clearCache() {
        try {
            strategyContext.clearCache();
            log.info("Upload cache cleared by admin");
            return Map.of(
                    "success", true,
                    "message", "✅ Cache đã được xóa"
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "❌ Lỗi xóa cache: " + e.getMessage()
            );
        }
    }

    /**
     * REST API: Clear upload history
     */
    @PostMapping("/api/clear-history")
    @ResponseBody
    public Map<String, Object> clearHistory() {
        try {
            strategyContext.clearHistory();
            log.info("Upload history cleared by admin");
            return Map.of(
                    "success", true,
                    "message", "✅ Lịch sử đã được xóa"
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "message", "❌ Lỗi xóa lịch sử: " + e.getMessage()
            );
        }
    }

    /**
     * Get strategy description
     */
    private String getStrategyDescription(String strategy) {
        switch (strategy) {
            case "CLOUDINARY_WITH_PROXY":
                return "Cloudinary + Cache + Logging + History (NHƯ CẦU)";
            case "CLOUDINARY":
                return "Cloudinary trực tiếp (không cache/log)";
            case "LOCAL_STORAGE":
                return "Lưu local server (dev/testing)";
            default:
                return "Không xác định";
        }
    }
}
