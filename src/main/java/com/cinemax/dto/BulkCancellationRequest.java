package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BulkCancellationRequest {
    @NotEmpty private List<String> bookingReferences;
    private String cancellationReason;
}
