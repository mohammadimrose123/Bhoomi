package com.bhoomi.land;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "locations")
public class Location extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String country = "India";

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false, length = 120)
    private String district;

    @Column(nullable = false, length = 120)
    private String mandal;

    @Column(nullable = false, length = 120)
    private String village;

    @Column(name = "postal_code", length = 16)
    private String postalCode;

    protected Location() {
    }

    public Location(String state, String district, String mandal, String village, String postalCode) {
        this.state = state;
        this.district = district;
        this.mandal = mandal;
        this.village = village;
        this.postalCode = postalCode;
    }

    public String getState() { return state; }
    public String getDistrict() { return district; }
    public String getMandal() { return mandal; }
    public String getVillage() { return village; }
    public String getPostalCode() { return postalCode; }
}
