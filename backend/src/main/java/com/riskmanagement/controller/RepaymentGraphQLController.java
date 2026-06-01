package com.riskmanagement.controller;

import com.riskmanagement.model.Repayment;
import com.riskmanagement.service.RepaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
public class RepaymentGraphQLController {
    @Autowired
    private RepaymentService repaymentService;

    @QueryMapping
    public List<Repayment> repayments() {
        return repaymentService.getAllRepayments();
    }

    @QueryMapping
    public Repayment repayment(@Argument Long id) {
        return repaymentService.getRepaymentById(id).orElse(null);
    }

    @QueryMapping
    public List<Repayment> repaymentsByLoan(@Argument Long loanId) {
        return repaymentService.getRepaymentsByLoanId(loanId);
    }

    @MutationMapping
    public Repayment createRepayment(@Argument RepaymentInput repayment) {
        Repayment r = toRepayment(repayment);
        return repaymentService.saveRepayment(r);
    }

    @MutationMapping
    public Repayment updateRepayment(@Argument Long id, @Argument RepaymentInput repayment) {
        Optional<Repayment> existing = repaymentService.getRepaymentById(id);
        if (existing.isEmpty()) return null;
        Repayment r = toRepayment(repayment);
        r.setRepaymentId(id);
        return repaymentService.saveRepayment(r);
    }

    @MutationMapping
    public Boolean deleteRepayment(@Argument Long id) {
        if (repaymentService.getRepaymentById(id).isEmpty()) return false;
        repaymentService.deleteRepayment(id);
        return true;
    }

    public static class RepaymentInput {
        public Long loanId;
        public String dueDate;
        public String paymentDate;
        public Double amountDue;
        public Double amountPaid;
        public Integer daysPastDue;
    }

    private Repayment toRepayment(RepaymentInput input) {
        Repayment r = new Repayment();
        r.setLoanId(input.loanId);
        if (input.dueDate != null) r.setDueDate(java.time.LocalDate.parse(input.dueDate));
        if (input.paymentDate != null) r.setPaymentDate(java.time.LocalDate.parse(input.paymentDate));
        r.setAmountDue(input.amountDue);
        r.setAmountPaid(input.amountPaid);
        r.setDaysPastDue(input.daysPastDue);
        return r;
    }
}
