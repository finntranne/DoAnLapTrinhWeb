package com.alotra.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
@Slf4j
public class PdfGeneratorService {

    @Autowired
    private TemplateEngine templateEngine;

    public byte[] generatePdf(String templateName, Map<String, Object> data) throws IOException {
        try {
            // 1. Chuẩn bị Context Thymeleaf
            Context context = new Context();
            context.setVariables(data);

            // 2. Render HTML từ template
            String htmlContent = templateEngine.process(templateName, context);

            // 3. Chuyển đổi HTML sang PDF
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            
            // TẠO ĐƯỜNG DẪN CƠ SỞ CHO CÁC TÀI NGUYÊN (QUAN TRỌNG CHO VIỆC TẢI FONT)
            // Giả định file HTML đã được render không cần đường dẫn cơ sở
            builder.withHtmlContent(htmlContent, "file:///"); // <-- Dùng "file:///" hoặc null

            // --- THÊM FONT HỖ TRỢ TIẾNG VIỆT ---
            ClassPathResource fontResource = new ClassPathResource("fonts/Roboto-Regular.ttf"); // THAY TÊN FONT CỦA BẠN
            if (fontResource.exists()) {
                try (InputStream fontStream = fontResource.getInputStream()) {
                    // Đăng ký font với tên "Roboto" (hoặc tên font bạn muốn)
                    builder.useFont(() -> fontStream, "Roboto");
                }
            } else {
                 log.warn("Không tìm thấy file font tại resources/fonts/. PDF có thể bị lỗi tiếng Việt.");
            }
            // ------------------------------------
            
            builder.toStream(os);
            builder.run();

            log.info("PDF generated successfully for template: {}", templateName);
            return os.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF for template: {}", templateName, e);
            throw new IOException("Không thể tạo file PDF: " + e.getMessage(), e);
        }
    }
}