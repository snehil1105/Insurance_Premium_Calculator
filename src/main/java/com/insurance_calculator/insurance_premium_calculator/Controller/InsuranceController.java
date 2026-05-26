package com.insurance_calculator.insurance_premium_calculator.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.insurance_calculator.insurance_premium_calculator.Model.PremiumResult;
import com.insurance_calculator.insurance_premium_calculator.Model.VehicleRequest;
import com.insurance_calculator.insurance_premium_calculator.Service.PremiumService;

@Controller
public class InsuranceController {

    @Autowired PremiumService premiumService;

    @GetMapping("/")
    public String home(){
        return "home";
    }

     @GetMapping("/form")
    public String showForm(@RequestParam String type, Model model) {

        VehicleRequest request = new VehicleRequest();
        request.setVehicleType(type);

        model.addAttribute("request", request);

        model.addAttribute("vehicleLabel", getVehicleLabel(type));

        switch (type) {
            case "TW":        return "forms/tw-form";
            case "PC":        return "forms/pc-form";
            case "COM_GOODS": return "forms/com-goods-form";
            case "PVT_GOODS": return "forms/pvt-goods-form";
            case "TAXI":      return "forms/taxi-form";
            case "3W_PASS":   return "forms/3w-pass-form";
            case "3W_GOODS":  return "forms/3w-goods-form";
            case "TRACTOR":   return "forms/tractor-form";
            default:          return "home";
        }
    }

     @PostMapping("/calculate")
    public String calculate(@ModelAttribute VehicleRequest request,Model model) {

        PremiumResult result = premiumService.calculate(request);

        model.addAttribute("result", result);
        model.addAttribute("request", request);

        return "result";
    }

     private String getVehicleLabel(String type) {
        switch (type) {
            case "TW":        return "Two Wheeler";
            case "PC":        return "Private Car";
            case "COM_GOODS": return "Commercial Goods Carrier";
            case "PVT_GOODS": return "Private Goods Carrier";
            case "TAXI":      return "Taxi (Upto 6 Passenger)";
            case "3W_PASS":   return "3 Wheeler Passenger";
            case "3W_GOODS":  return "3 Wheeler Goods Carrier";
            case "TRACTOR":   return "Tractor / Special Vehicle";
            default:          return "Vehicle";
        }
    }

}
