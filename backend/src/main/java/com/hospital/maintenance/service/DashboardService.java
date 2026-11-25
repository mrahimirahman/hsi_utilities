package com.hospital.maintenance.service;

import com.hospital.maintenance.domain.MaintenanceRequest;
import com.hospital.maintenance.dto.DashboardResponse;
import com.hospital.maintenance.repository.MaintenanceRequestRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final MaintenanceRequestRepository maintenanceRequestRepository;

    public DashboardService(MaintenanceRequestRepository maintenanceRequestRepository) {
        this.maintenanceRequestRepository = maintenanceRequestRepository;
    }

    public DashboardResponse getDashboard() {
        DashboardResponse response = new DashboardResponse();
        List<MaintenanceRequest> requests = maintenanceRequestRepository.findAll();
        List<Map<String, Object>> recentRequests = new ArrayList<Map<String, Object>>();

        for (MaintenanceRequest request : requests) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", request.getId());
            item.put("title", request.getTitle());
            item.put("location", request.getLocation());
            item.put("status", request.getStatus());
            item.put("priority", request.getPriority());
            item.put("assignedTeam", request.getAssignedTeam());
            recentRequests.add(item);
        }

        response.setTotalRequests(requests.size());
        response.setOpenRequests((int) maintenanceRequestRepository.countByStatus("OPEN"));
        response.setInProgressRequests((int) maintenanceRequestRepository.countByStatus("IN_PROGRESS"));
        response.setCompletedRequests((int) maintenanceRequestRepository.countByStatus("COMPLETED"));
        response.setRecentRequests(recentRequests);
        return response;
    }
}
