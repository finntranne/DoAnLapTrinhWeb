package com.alotra.entity.product;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "Categories")
public class Category {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CategoryID")
	private int categoryId;
	
	@Column(name = "CategoryName", columnDefinition = "NVARCHAR(255)")
	private String categoryName;
	
	 
	@Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
	private String description;
	
	@Column(name = "Status")
	private int status;

}
