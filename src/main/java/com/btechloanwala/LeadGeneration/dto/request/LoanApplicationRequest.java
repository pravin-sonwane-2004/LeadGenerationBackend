package com.btechloanwala.LeadGeneration.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload accepted from the React frontend for {@code POST /api/apply-now}.
 *
 * <p>This is deliberately decoupled from the {@code LoanApplication} entity: the
 * frontend contract (String {@code amount}) differs from the database contract
 * (DECIMAL {@code loanAmount}). Money is sent as a String by the UI and converted to
 * BigDecimal by the service layer — never blindly cast.</p>
 */
public class LoanApplicationRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name must be at most 120 characters")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must contain 10 digits.")
    private String mobile;

    @Email(message = "Invalid email address")
    @Size(max = 120, message = "Email must be at most 120 characters")
    private String email;

    @NotBlank(message = "Loan type is required")
    private String loanType;

    @NotBlank(message = "Loan amount is required")
    private String amount;

    @NotBlank(message = "Employment type is required")
    private String employment;

    @NotBlank(message = "Monthly income is required")
    private String income;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    private String message;

    @NotNull(message = "Consent is required to submit the application.")
    @AssertTrue(message = "Consent is required to submit the application.")
    private Boolean consent;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLoanType() {
        return loanType;
    }

    public void setLoanType(String loanType) {
        this.loanType = loanType;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getEmployment() {
        return employment;
    }

    public void setEmployment(String employment) {
        this.employment = employment;
    }

    public String getIncome() {
        return income;
    }

    public void setIncome(String income) {
        this.income = income;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getConsent() {
        return consent;
    }

    public void setConsent(Boolean consent) {
        this.consent = consent;
    }
}