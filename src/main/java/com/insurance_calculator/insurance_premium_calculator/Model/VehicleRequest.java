package com.insurance_calculator.insurance_premium_calculator.Model;

import lombok.Data;

@Data
public class VehicleRequest {

    private String vehicleType;

    private double cc;

    private double gvw;                       // GVW for trucks/goods carrier

    private double idv;                      // IDV = Insured Declared Value

    private double discountPercent;

    private double ncbPercent;              // NCB = No Claim Bonus

    private boolean paRequired;            // PA Cover = Personal Accident cover

    private boolean zeroDepthRequired;

    private int unnamedPassengers;

    private boolean llToDriver;

    private int unnamedLabour;

    private String vehicleAge;

    private String vehicleSubType;         // For 3W passenger — vehicle subtype (AUTO or E_RICKSHAW)

    private boolean trailerAttached;      // For tractor — whether trailer is attached (yes/no)

}
