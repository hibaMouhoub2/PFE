package com.example.projetpfe.entity;

import java.util.ArrayList;
import java.util.List;

public enum Branche {
    CASA_AZHAR("Casa Azhar", "SUP_BERNOUSSI_ZENATA"),
    CASA_DIAR_EL_JADIDA("Casa Diar El jadida", "SUP_FIDAA_SIDI_BELYOUT"),
    CASA_HAY_FARAH("Casa Hay Farah", "SUP_BENMSIK_SIDI_OTHMANE"),
    CASA_KOREA("Casa Koréa", "SUP_FIDAA_SIDI_BELYOUT");

    private final String displayName;
    private final String regionCode; // Code NMREG auquel cette branche appartient

    Branche(String displayName, String regionCode) {
        this.displayName = displayName;
        this.regionCode = regionCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRegionCode() {
        return regionCode;
    }


//    public static List<Branche> findByRegionCode(String regionCode) {
//        List<Branche> branches = new ArrayList<>();
//        for (Branche branche : Branche.values()) {
//            if (branche.getRegionCode().equals(regionCode)) {
//                branches.add(branche);
//            }
//        }
//        return branches;
//    }
}