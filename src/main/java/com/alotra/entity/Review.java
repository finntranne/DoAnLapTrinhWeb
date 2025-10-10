package com.alotra.entity;

      import com.alotra.entity.order.OrderDetail;
import com.alotra.entity.user.Customer;

import jakarta.persistence.*;
      import lombok.AllArgsConstructor;
      import lombok.Data;
      import lombok.NoArgsConstructor;

      @Entity
      @Table(name = "reviews")
      @Data
      @NoArgsConstructor
      @AllArgsConstructor
      public class Review {

          @Id
          @GeneratedValue(strategy = GenerationType.IDENTITY)
          @Column(name = "ReviewID")
          private Integer reviewId;

          @Column(name = "Rating")
          private Integer rating;

          @Column(name = "Comment")
          private String comment;

          @ManyToOne
          @JoinColumn(name = "orderDetailId", referencedColumnName = "OrderDetailID")
          private OrderDetail orderDetail;

          @ManyToOne
          @JoinColumn(name = "customerId", referencedColumnName = "CustomerID")
          private Customer customer;
      }