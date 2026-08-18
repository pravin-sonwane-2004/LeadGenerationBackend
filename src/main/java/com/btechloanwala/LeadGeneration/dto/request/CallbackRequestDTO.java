package com.btechloanwala.LeadGeneration.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload accepted from the React frontend for {@code POST /api/callback}.
 *
 * <p>Contact details only — no income/amount fields on this form. The loan type is a
 * plain String and validated by the service against {@link
 * com.btechloanwala.LeadGeneration.enums.LoanType}.</p>
 */
public class CallbackRequestDTO {

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name must be at most 120 characters")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must contain 10 digits.")
    private String mobile;

    @NotBlank(message = "Loan type is required")
    private String loanType;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getLoanType() {
        return loanType;
    }

    public void setLoanType(String loanType) {
        this.loanType = loanType;
    }
}