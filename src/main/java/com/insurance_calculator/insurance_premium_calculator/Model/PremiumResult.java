package com.insurance_calculator.insurance_premium_calculator.Model;

import lombok.Data;

@Data
public class PremiumResult {
    private String vehicleTypeLabel;


    private double idv;

    private double odPercent;           // OD rate % used

    private double odPremium;           // IDV × OD%

    private double discountPercent;     // discount given

    private double discountedOdp;       // after discount

    private double ncbPercent;          // NCB applied

    private double odAfterNcb;          // after NCB

    private double actPremium;          // Third Party (fixed)

    private double paPremium;           // PA cover amount

    private double zeroDepthPremium;    // Zero depth add-on

    private double unnamedPassengerPremium;  // unnamed passengers

    private double llPremium;           // LL to driver

    private double unnamedLabourPremium;    // unnamed labour (goods)

    private double totalBeforeGst;      // sum of everything before GST

    private double gstAmount;           // GST calculated

    private double netPremium;          // FINAL amount

    // If something went wrong (e.g. invalid input)
    private String errorMessage;
}
