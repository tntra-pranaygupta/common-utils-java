package io.tntra.common_utils.transaction.dto;

import io.tntra.common_utils.validation.annotations.ValidAmount;
import io.tntra.common_utils.validation.annotations.ValidPan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequestDto {

    @ValidPan
    private String panToken; // Tokenized PAN, might be fetched from vault

    @NotBlank(message = "panLastFour is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "panLastFour must be exactly 4 digits")
    private String panLastFour;

    @NotBlank(message = "cardScheme is required")
    private String cardScheme;

    @ValidAmount
    private BigDecimal amount;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a valid 3-letter ISO code")
    private String currency;

    private String correlationId;
}
