package com.alotra.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleCategoryDTO {
    private Integer categoryID;
    private String categoryName;
}