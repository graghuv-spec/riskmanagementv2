package com.riskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

@Entity
@Table(name = "repayments")
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long repaymentId;

    @NotNull(message = "loanId is required")
    private Long loanId;

    @NotNull(message = "dueDate is required")
    private LocalDate dueDate;

    private LocalDate paymentDate;

    @NotNull(message = "amountDue is required")
    @DecimalMin(value = "0.0", message = "amountDue must be 0 or greater")
    private Double amountDue;

    @NotNull(message = "amountPaid is required")
    @DecimalMin(value = "0.0", message = "amountPaid must be 0 or greater")
    private Double amountPaid;

    @NotNull(message = "daysPastDue is required")
    @PositiveOrZero(message = "daysPastDue must be 0 or greater")
    private Integer daysPastDue;

    // Getters and Setters
    public Long getRepaymentId() {
        return repaymentId;
    }

    public void setRepaymentId(Long repaymentId) {
        this.repaymentId = repaymentId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Double getAmountDue() {
        return amountDue;
    }

    public void setAmountDue(Double amountDue) {
        this.amountDue = amountDue;
    }

    public Double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Integer getDaysPastDue() {
        return daysPastDue;
    }

    public void setDaysPastDue(Integer daysPastDue) {
        this.daysPastDue = daysPastDue;
    }
}