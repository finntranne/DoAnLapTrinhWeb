package com.alotra.controller.vendor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alotra.dto.shop.CategoryRevenueDTO;
import com.alotra.dto.shop.ShopRevenueDTO;
import com.alotra.security.MyUserDetails;
import com.alotra.service.vendor.VendorShopRevenueService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/vendor")
@PreAuthorize("hasAuthority('VENDOR')")
@RequiredArgsConstructor
@Slf4j
public class VendorShopRevenueController {

	private final VendorShopRevenueService vendorShopRevenueService;

	// ==================== HELPER METHOD ====================

	/**
	 * Lấy shopId từ authenticated user Throw exception nếu user chưa có shop
	 */
	private Integer getShopIdOrThrow(@AuthenticationPrincipal MyUserDetails userDetails) {
		if (userDetails == null) {
			throw new IllegalStateException("User is not authenticated");
		}

		Integer shopId = userDetails.getShopId();

		if (shopId == null) {
			throw new IllegalStateException("Bạn chưa đăng ký shop. Vui lòng đăng ký shop trước.");
		}

		return shopId;
	}

	// ==================== REVENUE MANAGEMENT ====================

	@GetMapping("/revenue")
	public String viewRevenue(@AuthenticationPrincipal MyUserDetails userDetails,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
			Model model, RedirectAttributes redirectAttributes) {

		try {
			Integer shopId = getShopIdOrThrow(userDetails);

			// Fetch data (defaulting logic is inside service if needed)
			List<ShopRevenueDTO> revenues = vendorShopRevenueService.getShopRevenue(shopId, startDate, endDate);

			model.addAttribute("revenues", revenues);
			List<CategoryRevenueDTO> categoryRevenues = vendorShopRevenueService.getShopRevenueByCategory(shopId, startDate,
					endDate);
			model.addAttribute("categoryRevenues", categoryRevenues);
			// Pass raw dates back for the form
			model.addAttribute("startDate", startDate);
			model.addAttribute("endDate", endDate);

			return "vendor/revenue";

		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/shop/register";
		} catch (Exception e) {
			log.error("Error loading revenue page", e);
			redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi tải trang doanh thu.");
			return "redirect:/vendor/dashboard";
		}
	}

	@GetMapping("/revenue/export")
		public void exportRevenueToExcel(@AuthenticationPrincipal MyUserDetails userDetails,
				@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
				@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
				HttpServletResponse response) {

			log.info("Exporting revenue to Excel for shopId: {}, startDate: {}, endDate: {}",
					(userDetails != null ? userDetails.getShopId() : "N/A"), startDate, endDate);

			try {
				Integer shopId = getShopIdOrThrow(userDetails);

				// 1️⃣ Lấy dữ liệu
				List<ShopRevenueDTO> revenues = vendorShopRevenueService.getShopRevenue(shopId, startDate, endDate);
				log.info("Fetched {} revenue records for export.", revenues.size());

				// 2️⃣ Tạo Workbook & Sheet
				Workbook workbook = new XSSFWorkbook();
				Sheet sheet = workbook.createSheet("Doanh thu");

				// 3️⃣ Header Row
				Row headerRow = sheet.createRow(0);
				String[] headers = { "Ngày", "Số đơn hàng", "Doanh thu (Gross)", "Phí (Commission)", "Thực nhận (Net)" };

				CellStyle headerStyle = workbook.createCellStyle();
				Font headerFont = workbook.createFont();
				headerFont.setBold(true);
				headerStyle.setFont(headerFont);
				headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
				headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
				headerStyle.setAlignment(HorizontalAlignment.CENTER);

				for (int i = 0; i < headers.length; i++) {
					Cell cell = headerRow.createCell(i);
					cell.setCellValue(headers[i]);
					cell.setCellStyle(headerStyle);
				}

				// 4️⃣ Dòng dữ liệu
				int rowNum = 1;
				DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

				CreationHelper createHelper = workbook.getCreationHelper();

				CellStyle currencyCellStyle = workbook.createCellStyle();
				currencyCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0\" ₫\""));

				for (ShopRevenueDTO revenue : revenues) {
					Row row = sheet.createRow(rowNum++);

					row.createCell(0).setCellValue(revenue.getDate().format(dateFormatter));
					row.createCell(1).setCellValue(revenue.getTotalOrders());

					Cell grossCell = row.createCell(2);
					grossCell.setCellValue(revenue.getOrderAmount().doubleValue());
					grossCell.setCellStyle(currencyCellStyle);

					Cell commissionCell = row.createCell(3);
					commissionCell.setCellValue(revenue.getCommissionAmount().doubleValue());
					commissionCell.setCellStyle(currencyCellStyle);

					Cell netCell = row.createCell(4);
					netCell.setCellValue(revenue.getNetRevenue().doubleValue());
					netCell.setCellStyle(currencyCellStyle);
				}

				// 5️⃣ Tổng cộng
				Row totalRow = sheet.createRow(rowNum);
				Font totalFont = workbook.createFont();
				totalFont.setBold(true);

				CellStyle totalStyle = workbook.createCellStyle();
				totalStyle.setFont(totalFont);
				totalStyle.setDataFormat(currencyCellStyle.getDataFormat());

				Cell totalLabelCell = totalRow.createCell(1);
				totalLabelCell.setCellValue("Tổng cộng:");
				totalLabelCell.setCellStyle(headerStyle);

				double totalGross = revenues.stream().mapToDouble(r -> r.getOrderAmount().doubleValue()).sum();
				double totalCommission = revenues.stream().mapToDouble(r -> r.getCommissionAmount().doubleValue()).sum();
				double totalNet = revenues.stream().mapToDouble(r -> r.getNetRevenue().doubleValue()).sum();

				Cell totalGrossCell = totalRow.createCell(2);
				totalGrossCell.setCellValue(totalGross);
				totalGrossCell.setCellStyle(totalStyle);

				Cell totalCommissionCell = totalRow.createCell(3);
				totalCommissionCell.setCellValue(totalCommission);
				totalCommissionCell.setCellStyle(totalStyle);

				Cell totalNetCell = totalRow.createCell(4);
				totalNetCell.setCellValue(totalNet);
				totalNetCell.setCellStyle(totalStyle);

				// 6️⃣ Auto-size cột
				for (int i = 0; i < headers.length; i++) {
					sheet.autoSizeColumn(i);
				}

				// 7️⃣ Gửi file về client
				String filename = "BaoCaoDoanhThu_"
						+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";
				response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

				workbook.write(response.getOutputStream());
				workbook.close();

				log.info("Successfully exported revenue data to Excel file: {}", filename);

			} catch (IllegalStateException e) {
				log.error("Authentication/Authorization error during export: {}", e.getMessage(), e);
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			} catch (IOException e) {
				log.error("IOException during Excel export: {}", e.getMessage(), e);
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} catch (Exception e) {
				log.error("Unexpected error during Excel export", e);
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
}