package com.insurance_calculator.insurance_premium_calculator.Service;

import com.insurance_calculator.insurance_premium_calculator.Model.VehicleRequest;
import com.insurance_calculator.insurance_premium_calculator.Model.PremiumResult;
import org.springframework.stereotype.Service;

@Service
public class PremiumService {


    // -------------------------------------------------------
    // MAIN METHOD — Controller will call this
    // -------------------------------------------------------
    public PremiumResult calculate(VehicleRequest request) {
        switch (request.getVehicleType()) {
            case "TW":        return calculateTW(request);
            case "PC":        return calculatePC(request);
            case "COM_GOODS": return calculateComGoods(request);
            case "PVT_GOODS": return calculatePvtGoods(request);
            case "TAXI":      return calculateTaxi(request);
            case "3W_PASS":   return calculate3WPassenger(request);
            case "3W_GOODS":  return calculate3WGoods(request);
            case "TRACTOR":   return calculateTractor(request);
            default:
                PremiumResult error = new PremiumResult();
                error.setErrorMessage("Invalid vehicle type selected.");
                return error;
        }
    }

    // -------------------------------------------------------
    // IDV — user enters directly, no ex-showroom needed
    // -------------------------------------------------------
    private double calculateIDV(VehicleRequest req) {
        return req.getIdv();
    }

    // -------------------------------------------------------
    // TWO WHEELER
    // OD% depends on CC + Age (3 age slabs, 4 CC slabs)
    // ACT is fixed by IRDAI based on CC
    // -------------------------------------------------------
    private PremiumResult calculateTW(VehicleRequest req) {

        PremiumResult result = new PremiumResult();
        result.setVehicleTypeLabel("Two Wheeler");

        double idv        = calculateIDV(req);
        double odPercent  = getTwOdPercent(req.getCc(), req.getVehicleAge());
        double act        = getTwAct(req.getCc());

        double odPremium      = idv * odPercent / 100;
        double discountedOdp  = odPremium * (1 - req.getDiscountPercent() / 100);
        double odAfterNcb     = discountedOdp * (1 - req.getNcbPercent() / 100);

        // Zero Depth — from your sheet: % value column
        // Using 20% of OD as default; adjust if your sheet has exact slab
        double zeroDepthPremium = 0;
        if (req.isZeroDepthRequired()) {
            zeroDepthPremium = odPremium * 0.20;
        }

        double paPremium  = req.isPaRequired() ? 275.0 : 0.0;

        double total      = odAfterNcb + act + zeroDepthPremium + paPremium;
        double gst        = total * 0.18;
        double netPremium = total + gst;

        result.setIdv(idv);
        result.setOdPercent(odPercent);
        result.setOdPremium(round(odPremium));
        result.setDiscountPercent(req.getDiscountPercent());
        result.setDiscountedOdp(round(discountedOdp));
        result.setNcbPercent(req.getNcbPercent());
        result.setOdAfterNcb(round(odAfterNcb));
        result.setActPremium(act);
        result.setPaPremium(paPremium);
        result.setZeroDepthPremium(round(zeroDepthPremium));
        result.setTotalBeforeGst(round(total));
        result.setGstAmount(round(gst));
        result.setNetPremium(round(netPremium));
        return result;
    }

    // -------------------------------------------------------
    // PRIVATE CAR
    // OD% depends on CC + Age (3 age slabs, 3 CC slabs)
    // -------------------------------------------------------
    private PremiumResult calculatePC(VehicleRequest req) {

        PremiumResult result = new PremiumResult();
        result.setVehicleTypeLabel("Private Car");

        double idv        = calculateIDV(req);
        double odPercent  = getPcOdPercent(req.getCc(), req.getVehicleAge());
        double act        = getPcAct(req.getCc());

        double odPremium      = idv * odPercent / 100;
        double discountedOdp  = odPremium * (1 - req.getDiscountPercent() / 100);
        double odAfterNcb     = discountedOdp * (1 - req.getNcbPercent() / 100);

        double zeroDepthPremium = 0;
        if (req.isZeroDepthRequired()) {
            zeroDepthPremium = odPremium * 0.20;
        }

        double paPremium = req.isPaRequired() ? 275.0 : 0.0;

        // Unnamed passengers: 200 per 4 passengers
        double unnamedPassengerPremium = 0;
        if (req.getUnnamedPassengers() > 0) {
            unnamedPassengerPremium = Math.ceil(req.getUnnamedPassengers() / 4.0) * 200;
        }

        double llPremium  = req.isLlToDriver() ? 50.0 : 0.0;

        double total      = odAfterNcb + act + zeroDepthPremium
                          + paPremium + unnamedPassengerPremium + llPremium;
        double gst        = total * 0.18;
        double netPremium = total + gst;

        result.setIdv(idv);
        result.setOdPercent(odPercent);
        result.setOdPremium(round(odPremium));
        result.setDiscountPercent(req.getDiscountPercent());
        result.setDiscountedOdp(round(discountedOdp));
        result.setNcbPercent(req.getNcbPercent());
        result.setOdAfterNcb(round(odAfterNcb));
        result.setActPremium(act);
        result.setPaPremium(paPremium);
        result.setZeroDepthPremium(round(zeroDepthPremium));
        result.setUnnamedPassengerPremium(round(unnamedPassengerPremium));
        result.setLlPremium(llPremium);
        result.setTotalBeforeGst(round(total));
        result.setGstAmount(round(gst));
        result.setNetPremium(round(netPremium));
        return result;
    }

    // -------------------------------------------------------
    // COMMERCIAL GOODS CARRIER
    // OD% depends on Age only (GVW decides ACT, not OD%)
    // GST: 12% on ACT, 18% on rest
    // -------------------------------------------------------
    private PremiumResult calculateComGoods(VehicleRequest req) {

        PremiumResult result = new PremiumResult();
        result.setVehicleTypeLabel("Commercial Goods Carrier");

        double idv        = calculateIDV(req);
        double odPercent  = getComGoodsOdPercent(req.getVehicleAge());
        double act        = getComGoodsAct(req.getGvw());

        double odPremium  = idv * odPercent / 100;

        // Extra GVW charge: if GVW > 12000, extra @ 27 per quintal
        double extraOdp = 0;
        if (req.getGvw() > 12000) {
            double extraGvw = req.getGvw() - 12000;
            extraOdp = (extraGvw / 100) * 27;
        }

        double discountedOdp  = (odPremium + extraOdp) * (1 - req.getDiscountPercent() / 100);
        double odAfterNcb     = discountedOdp * (1 - req.getNcbPercent() / 100);

        double paPremium              = req.isPaRequired() ? 275.0 : 0.0;
        double unnamedPassengerPremium = req.getUnnamedPassengers() * 50.0;
        double unnamedLabourPremium    = req.getUnnamedLabour() * 50.0;
        double llPremium               = req.isLlToDriver() ? 50.0 : 0.0;

        double total = odAfterNcb + act + paPremium
                     + unnamedPassengerPremium + unnamedLabourPremium + llPremium;

        // GST: 12% on ACT part, 18% on OD and add-ons
        double gst = (act * 0.12) + ((total - act) * 0.18);

        double netPremium = total + gst;

        result.setIdv(idv);
        result.setOdPercent(odPercent);
        result.setOdPremium(round(odPremium));
        result.setDiscountPercent(req.getDiscountPercent());
        result.setDiscountedOdp(round(discountedOdp));
        result.setNcbPercent(req.getNcbPercent());
        result.setOdAfterNcb(round(odAfterNcb));
        result.setActPremium(act);
        result.setPaPremium(paPremium);
        result.setUnnamedPassengerPremium(round(unnamedPassengerPremium));
        result.setUnnamedLabourPremium(round(unnamedLabourPremium));
        result.setLlPremium(llPremium);
        result.setTotalBeforeGst(round(total));
        result.setGstAmount(round(gst));
        result.setNetPremium(round(netPremium));
        return result;
    }

    // -------------------------------------------------------
    // PRIVATE GOODS CARRIER
    // Same structure as COM GOODS but different OD%
    // -------------------------------------------------------
    private PremiumResult calculatePvtGoods(VehicleRequest req) {

        PremiumResult result = new PremiumResult();
        result.setVehicleTypeLabel("Private Goods Carrier");

        double idv        = calculateIDV(req);
        double odPercent  = getPvtGoodsOdPercent(req.getVehicleAge());
        double act        = getComGoodsAct(req.getGvw()); // same ACT table as COM

        double odPremium  = idv * odPercent / 100;

        double extraOdp = 0;
        if (req.getGvw() > 12000) {
            double extraGvw = req.getGvw() - 12000;
            extraOdp = (extraGvw / 100) * 27;
        }

        double discountedOdp  = (odPremium + extraOdp) * (1 - req.getDiscountPercent() / 100);
        double odAfterNcb     = discountedOdp * (1 - req.getNcbPercent() / 100);

        double paPremium               = req.isPaRequired() ? 275.0 : 0.0;
        double unnamedPassengerPremium  = req.getUnnamedPassengers() * 50.0;
        double unnamedLabourPremium     = req.getUnnamedLabour() * 50.0;
        double llPremium                = req.isLlToDriver() ? 50.0 : 0.0;

        double total = odAfterNcb + act + paPremium
                     + unnamedPassengerPremium + unnamedLabourPremium + llPremium;

        double gst        = (act * 0.12) + ((total - act) * 0.18);
        double netPremium = total + gst;

        result.setIdv(idv);
        result.setOdPercent(odPercent);
        result.setOdPremium(round(odPremium));
        result.setDiscountPercent(req.getDiscountPercent());
        result.setDiscountedOdp(round(discountedOdp));
        result.setNcbPercent(req.getNcbPercent());
        result.setOdAfterNcb(round(odAfterNcb));
        result.setActPremium(act);
        result.setPaPremium(paPremium);
        result.setUnnamedPassengerPremium(round(unnamedPassengerPremium));
        result.setUnnamedLabourPremium(round(unnamedLabourPremium));
        result.setLlPremium(llPremium);
        result.setTotalBeforeGst(round(total));
        result.setGstAmount(round(gst));
        result.setNetPremium(round(netPremium));
        return result;
    }

    // -------------------------------------------------------
    // TAXI UPTO 6 PASSENGER
    // OD% depends on CC + Age
    // GST: 12% on TP, 18% on rest
    // -------------------------------------------------------
    private PremiumResult calculateTaxi(VehicleRequest req) {

        PremiumResult result = new PremiumResult();
        result.setVehicleTypeLabel("Taxi (Upto 6 Passenger)");

        double idv        = calculateIDV(req);
        double odPercent  = getTaxiOdPercent(req.getCc(), req.getVehicleAge());
        double act        = getTaxiAct(req.getCc());

        double odPremium      = idv * odPercent / 100;
        double discountedOdp  = odPremium * (1 - req.getDiscountPercent() / 100);
        double odAfterNcb     = discountedOdp * (1 - req.getNcbPercent() / 100);

        // 978 per passenger from your sheet
        double unnamedPassengerPremium = req.getUnnamedPassengers() * 978.0;
        double paPremium  = req.isPaRequired() ? 275.0 : 0.0;
        double llPremium  = req.isLlToDriver() ? 50.0 : 0.0;

        double total      = odAfterNcb + act + paPremium
                          + unnamedPassengerPremium + llPremium;

        // GST: 12% on TP (act + passengers), 18% on rest
        double tpComponent = act + unnamedPassengerPremium;
        double gst         = (tpComponent * 0.12) + ((total - tpComponent) * 0.18);
        double netPremium  = total + gst;

        result.setIdv(idv);
        result.setOdPercent(odPercent);
        result.setOdPremium(round(odPremium));
        result.setDiscountPercent(req.getDiscountPercent());
        result.setDiscountedOdp(round(discountedOdp));
        result.setNcbPercent(req.getNcbPercent());
        result.setOdAfterNcb(round(odAfterNcb));
        result.setActPremium(act);
        result.setPaPremium(paPremium);
        result.setUnnamedPassengerPremium(round(unnamedPassengerPremium));
        result.setLlPremium(llPremium);
        result.setTotalBeforeGst(round(total));
        result.setGstAmount(round(gst));
        result.setNetPremium(round(netPremium));
        return result;
    }

    // -------------------------------------------------------
    // 3 WHEELER PASSENGER (Auto / E-Rickshaw)
    // OD% depends on passengers count + Age
    // Passenger rate: upto6 → 1214, 7-17 → 872, >17 → 806
    // -------------------------------------------------------
    private PremiumResult calculate3WPassenger(VehicleRequest req) {

        PremiumResult result = new PremiumResult();
        result.setVehicleTypeLabel("3 Wheeler Passenger (Auto/E-Rickshaw)");

        double idv       = calculateIDV(req);
        double odPercent = get3wPassOdPercent(req.getUnnamedPassengers(), req.getVehicleAge());

        // ACT: AUTO → 2539, E-RICKSHAW → different
        double act = "AUTO".equals(req.getVehicleSubType()) ? 2539.0 : 2000.0;

        double odPremium      = idv * odPercent / 100;
        double discountedOdp  = odPremium * (1 - req.getDiscountPercent() / 100);
        double odAfterNcb     = discountedOdp * (1 - req.getNcbPercent() / 100);

        // Passenger rate from your sheet
        double passengerRate;
        if (req.getUnnamedPassengers() <= 6)       passengerRate = 1214.0;
        else if (req.getUnnamedPassengers() <= 17)  passengerRate = 872.0;
        else                                        passengerRate = 806.0;

        double unnamedPassengerPremium = req.getUnnamedPassengers() * passengerRate;
        double paPremium  = req.isPaRequired() ? 275.0 : 0.0;
        double llPremium  = req.isLlToDriver() ? 50.0 : 0.0;

        double total      = odAfterNcb + act + paPremium
                          + unnamedPassengerPremium + llPremium;
        double gst        = total * 0.18;
        double netPremium = total + gst;

        result.setIdv(idv);
        result.setOdPercent(odPercent);
        result.setOdPremium(round(odPremium));
        result.setDiscountPercent(req.getDiscountPercent());
        result.setDiscountedOdp(round(discountedOdp));
        result.setNcbPercent(req.getNcbPercent());
        result.setOdAfterNcb(round(odAfterNcb));
        result.setActPremium(act);
        result.setPaPremium(paPremium);
        result.setUnnamedPassengerPremium(round(unnamedPassengerPremium));
        result.setLlPremium(llPremium);
        result.setTotalBeforeGst(round(total));
        result.setGstAmount(round(gst));
        result.setNetPremium(round(netPremium));
        return result;
    }

    // -------------------------------------------------------
    // 3 WHEELER GOODS CARRIER
    // Commercial type has no OD (only ACT)
    // Private type has OD%
    // -------------------------------------------------------
    private PremiumResult calculate3WGoods(VehicleRequest req) {

        PremiumResult result = new PremiumResult();
        result.setVehicleTypeLabel("3 Wheeler Goods Carrier");

        double idv       = calculateIDV(req);
        double odPercent = get3wGoodsOdPercent(req.getVehicleSubType(), req.getVehicleAge());
        double act       = 4492.0; // fixed from your sheet

        double odPremium      = idv * odPercent / 100;
        double discountedOdp  = odPremium * (1 - req.getDiscountPercent() / 100);
        double odAfterNcb     = discountedOdp * (1 - req.getNcbPercent() / 100);

        double paPremium  = req.isPaRequired() ? 275.0 : 0.0;
        double llPremium  = req.isLlToDriver() ? 50.0 : 0.0;

        double total      = odAfterNcb + act + paPremium + llPremium;
        double gst        = total * 0.18;
        double netPremium = total + gst;

        result.setIdv(idv);
        result.setOdPercent(odPercent);
        result.setOdPremium(round(odPremium));
        result.setDiscountPercent(req.getDiscountPercent());
        result.setDiscountedOdp(round(discountedOdp));
        result.setNcbPercent(req.getNcbPercent());
        result.setOdAfterNcb(round(odAfterNcb));
        result.setActPremium(act);
        result.setPaPremium(paPremium);
        result.setLlPremium(llPremium);
        result.setTotalBeforeGst(round(total));
        result.setGstAmount(round(gst));
        result.setNetPremium(round(netPremium));
        return result;
    }

    // -------------------------------------------------------
    // TRACTOR / SPECIAL VEHICLE
    // OD% depends on Age only (3 slabs)
    // -------------------------------------------------------
    private PremiumResult calculateTractor(VehicleRequest req) {

        PremiumResult result = new PremiumResult();
        result.setVehicleTypeLabel("Tractor / Special Vehicle");

        double idv       = calculateIDV(req);
        double odPercent = getTractorOdPercent(req.getVehicleAge());
        double act       = 6847.0; // fixed from your sheet

        double odPremium      = idv * odPercent / 100;
        double discountedOdp  = odPremium * (1 - req.getDiscountPercent() / 100);
        double odAfterNcb     = discountedOdp * (1 - req.getNcbPercent() / 100);

        double paPremium  = req.isPaRequired() ? 275.0 : 0.0;
        double llPremium  = req.isLlToDriver() ? 50.0 : 0.0;

        double total      = odAfterNcb + act + paPremium + llPremium;
        double gst        = total * 0.18;
        double netPremium = total + gst;

        result.setIdv(idv);
        result.setOdPercent(odPercent);
        result.setOdPremium(round(odPremium));
        result.setDiscountPercent(req.getDiscountPercent());
        result.setDiscountedOdp(round(discountedOdp));
        result.setNcbPercent(req.getNcbPercent());
        result.setOdAfterNcb(round(odAfterNcb));
        result.setActPremium(act);
        result.setPaPremium(paPremium);
        result.setLlPremium(llPremium);
        result.setTotalBeforeGst(round(total));
        result.setGstAmount(round(gst));
        result.setNetPremium(round(netPremium));
        return result;
    }


    // =======================================================
    // OD% LOOKUP TABLES — CC + AGE per vehicle type
    // =======================================================

    private double getTwOdPercent(double cc, String age) {
        if ("<=5".equals(age)) {
            if (cc <= 150)  return 1.676;
            if (cc <= 350)  return 1.802;
            return 1.844;
        } else if ("5-10".equals(age)) {
            if (cc <= 150)  return 1.760;
            if (cc <= 350)  return 1.848;
            return 1.936;
        } else {
            if (cc <= 150)  return 1.802;
            if (cc <= 350)  return 1.892;
            return 1.982;
        }
    }

    private double getPcOdPercent(double cc, String age) {
        if ("<=5".equals(age)) {
            if (cc <= 1000) return 3.039;
            if (cc <= 1500) return 3.191;
            return 3.343;
        } else if ("5-10".equals(age)) {
            if (cc <= 1000) return 3.191;
            if (cc <= 1500) return 3.351;
            return 3.510;
        } else {
            if (cc <= 1000) return 3.267;
            if (cc <= 1500) return 3.430;
            return 3.594;
        }
    }

    private double getTaxiOdPercent(double cc, String age) {
        if ("<=5".equals(age)) {
            if (cc <= 1000) return 3.191;
            if (cc <= 1500) return 3.351;
            return 3.510;
        } else if ("5-10".equals(age)) {
            if (cc <= 1000) return 3.271;
            if (cc <= 1500) return 3.435;
            return 3.598;
        } else {
            if (cc <= 1000) return 3.351;
            if (cc <= 1500) return 3.519;
            return 3.686;
        }
    }

    private double getComGoodsOdPercent(String age) {
        if ("<=5".equals(age))  return 1.726;
        if ("5-7".equals(age))  return 1.770;
        return 1.812;
    }

    private double getPvtGoodsOdPercent(String age) {
        if ("<=5".equals(age))  return 1.208;
        if ("5-7".equals(age))  return 1.239;
        return 1.268;
    }

    private double get3wPassOdPercent(int passengers, String age) {
        if ("<=5".equals(age)) {
            return passengers <= 6 ? 1.260 : 1.759;
        } else if ("5-7".equals(age)) {
            return passengers <= 6 ? 1.292 : 1.803;
        } else {
            return passengers <= 6 ? 1.323 : 1.847;
        }
    }

    private double get3wGoodsOdPercent(String subType, String age) {
        boolean isCom = "COMMERCIAL".equals(subType);
        if ("<=5".equals(age))  return isCom ? 1.640 : 1.148;
        if ("5-7".equals(age))  return isCom ? 1.681 : 1.177;
        return isCom ? 1.722 : 1.205;
    }

    private double getTractorOdPercent(String age) {
        if ("<=5".equals(age))  return 1.19;
        if ("5-7".equals(age))  return 1.22;
        return 1.25;
    }

    // =======================================================
    // ACT (Third Party) LOOKUP — fixed by IRDAI
    // =======================================================

    private double getTwAct(double cc) {
        // All CC ranges → same ACT for TW
        return 1366.0;
    }

    private double getPcAct(double cc) {
        if (cc <= 1000) return 2094.0;
        if (cc <= 1500) return 3416.0;
        return 7897.0;
    }

    private double getTaxiAct(double cc) {
        if (cc <= 1000) return 5765.0;
        if (cc <= 1500) return 7940.0;
        return 10423.0;
    }

    private double getComGoodsAct(double gvw) {
        if (gvw <= 7500)  return 16049.0;
        if (gvw <= 12000) return 24305.0;
        if (gvw <= 20000) return 35765.0;
        if (gvw <= 40000) return 44242.0;
        return 54159.0;
    }

    // =======================================================
    // UTILITY
    // =======================================================

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}