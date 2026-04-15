package com.cinemax.service;

import com.cinemax.dto.*;
import java.util.List;

public interface TheatreService {
    TheatreResponse onboardTheatre(TheatreOnboardRequest request, Long partnerId);
    List<TheatreResponse> getTheatresByPartner(Long partnerId);
}
