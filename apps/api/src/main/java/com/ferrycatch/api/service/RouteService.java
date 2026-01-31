package com.ferrycatch.api.service;

import com.ferrycatch.api.db.repo.RouteWithNextRepository;
import com.ferrycatch.api.dto.FerryDtos;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {
    private final RouteWithNextRepository repo;

    public RouteService(RouteWithNextRepository repo) {
        this.repo = repo;
    }

    public List<FerryDtos.RouteWithNextDto> listRoutesWithNext(String from, String to, String operator) {
        return repo.listRoutesWithNext(from, to, operator);
    }
}
