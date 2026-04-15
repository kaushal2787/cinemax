package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;
import com.cinemax.model.ShowStatus;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShowResponse {
    private Long showId;
    private LocalDateTime showTime;
    private String showTimeDisplay;
    private String showPeriod; // MORNING, AFTERNOON, EVENING, NIGHT
    private ShowStatus status;

    // Theatre info
    private Long theatreId;
    private String theatreName;
    private String theatreAddress;
    private String city;

    // Screen info
    private String screenName;
    private String screenType;

    // Movie info
    private Long movieId;
    private String movieTitle;
    private String language;
    private String certification;

    // Availability
    private Integer availableSeats;
    private Integer totalSeats;
    private Boolean isFastFilling;
    private Boolean isSoldOut;

    // Pricing
    private BigDecimal silverPrice;
    private BigDecimal goldPrice;
    private BigDecimal platinumPrice;
    private BigDecimal reclinePrice;

    // Offers applicable
    private List<OfferSummary> applicableOffers;
}
