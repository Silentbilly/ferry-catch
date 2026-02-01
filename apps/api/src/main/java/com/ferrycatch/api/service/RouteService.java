package com.ferrycatch.api.service;

import com.ferrycatch.api.dto.FerryDtos;
import com.ferrycatch.api.db.repo.RouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {
    private final RouteRepository routeRepo;

    public RouteService(RouteRepository routeRepo) {
        this.routeRepo = routeRepo;
    }

    public List<FerryDtos.RouteDto> listAllRoutes() {
        return routeRepo.listAll();
    }
}
