package com.example.rentalservice.api;

import com.example.rentalservice.api.dto.CreateServiceRequestPayload;
import com.example.rentalservice.api.dto.DeleteHistoryResponse;
import com.example.rentalservice.api.dto.EquipmentOptionResponse;
import com.example.rentalservice.api.dto.GeocodingEstimateResponse;
import com.example.rentalservice.api.dto.InvoiceResponse;
import com.example.rentalservice.api.dto.SavedServiceResponse;
import com.example.rentalservice.api.dto.ServiceRequestResponse;
import com.example.rentalservice.api.dto.UpdateServiceRequestPayload;
import com.example.rentalservice.service.BillingService;
import com.example.rentalservice.service.CurrentUserService;
import com.example.rentalservice.service.GeocodingService;
import com.example.rentalservice.service.UserServiceFlowService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserServiceFlowController {

    private final CurrentUserService currentUserService;
    private final UserServiceFlowService userServiceFlowService;
    private final BillingService billingService;
    private final GeocodingService geocodingService;

    public UserServiceFlowController(
            CurrentUserService currentUserService,
            UserServiceFlowService userServiceFlowService,
            BillingService billingService,
            GeocodingService geocodingService
    ) {
        this.currentUserService = currentUserService;
        this.userServiceFlowService = userServiceFlowService;
        this.billingService = billingService;
        this.geocodingService = geocodingService;
    }

    @GetMapping("/saved-services")
    public List<SavedServiceResponse> getSaved(Authentication authentication) {
        return userServiceFlowService.getSaved(currentUserService.getCurrentUser(authentication));
    }

    @PostMapping("/saved-services/{serviceId}")
    public Map<String, String> addToSaved(@PathVariable Long serviceId, Authentication authentication) {
        userServiceFlowService.addToSaved(currentUserService.getCurrentUser(authentication), serviceId);
        return Map.of("message", "Service added to saved list");
    }

    @DeleteMapping("/saved-services/{serviceId}")
    public Map<String, String> removeFromSaved(@PathVariable Long serviceId, Authentication authentication) {
        userServiceFlowService.removeFromSaved(currentUserService.getCurrentUser(authentication), serviceId);
        return Map.of("message", "Service removed from saved list");
    }

    @PostMapping("/service-requests")
    public ServiceRequestResponse createRequest(
            @Valid @RequestBody CreateServiceRequestPayload payload,
            Authentication authentication
    ) {
        return userServiceFlowService.createRequest(currentUserService.getCurrentUser(authentication), payload);
    }

    @GetMapping("/service-requests/geocoding-estimate")
    public GeocodingEstimateResponse geocodingEstimate(@RequestParam String address) {
        return geocodingService.estimateForAddress(address);
    }

    @GetMapping("/service-requests/geocoding-estimate-by-point")
    public GeocodingEstimateResponse geocodingEstimateByPoint(@RequestParam double lat, @RequestParam double lng) {
        return geocodingService.estimateForPoint(lat, lng);
    }

    @GetMapping("/service-requests")
    public List<ServiceRequestResponse> getMyRequests(Authentication authentication) {
        return userServiceFlowService.getMyRequests(currentUserService.getCurrentUser(authentication));
    }

    @GetMapping("/equipment/available")
    public List<EquipmentOptionResponse> listAvailableEquipment(
            @RequestParam(required = false) Long serviceId,
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate
    ) {
        return userServiceFlowService.listAvailableEquipment(serviceId, fromDate, toDate);
    }

    @PatchMapping("/service-requests/{requestId}")
    public ServiceRequestResponse updateRequest(
            @PathVariable Long requestId,
            @RequestBody UpdateServiceRequestPayload payload,
            Authentication authentication
    ) {
        return userServiceFlowService.updateRequest(currentUserService.getCurrentUser(authentication), requestId, payload);
    }

    @DeleteMapping("/service-requests/{requestId}")
    public ServiceRequestResponse cancelRequest(@PathVariable Long requestId, Authentication authentication) {
        return userServiceFlowService.cancelRequest(currentUserService.getCurrentUser(authentication), requestId);
    }

    @PostMapping("/service-requests/{requestId}/return")
    public ServiceRequestResponse markReturned(@PathVariable Long requestId, Authentication authentication) {
        return userServiceFlowService.markReturned(currentUserService.getCurrentUser(authentication), requestId);
    }

    @DeleteMapping("/service-requests/{requestId}/history")
    public Map<String, String> deleteHistoryRequest(@PathVariable Long requestId, Authentication authentication) {
        userServiceFlowService.deleteHistoryRequest(currentUserService.getCurrentUser(authentication), requestId);
        return Map.of("message", "Request removed from history");
    }

    @DeleteMapping("/service-requests/history")
    public DeleteHistoryResponse clearHistory(Authentication authentication) {
        int deleted = userServiceFlowService.clearHistory(currentUserService.getCurrentUser(authentication));
        return new DeleteHistoryResponse(deleted);
    }

    @GetMapping("/invoices")
    public List<InvoiceResponse> getMyInvoices(Authentication authentication) {
        return billingService.getMyInvoices(currentUserService.getCurrentUser(authentication));
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    public InvoiceResponse payInvoice(@PathVariable Long invoiceId, Authentication authentication) {
        return billingService.payInvoice(currentUserService.getCurrentUser(authentication), invoiceId);
    }

}
