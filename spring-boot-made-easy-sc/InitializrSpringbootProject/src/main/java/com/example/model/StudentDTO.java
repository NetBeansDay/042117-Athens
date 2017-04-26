/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.model;

import java.io.Serializable;


/**
 *
 * @author DiakogiannisA
 */
public class StudentDTO implements Serializable{


    private String firstname;
    private String lastname;

    public StudentDTO() {
    }

    
    
    public StudentDTO(String firstname, String lastname) {
        this.firstname = firstname;
        this.lastname = lastname;
    }

    


    public String getFirstName() {
        return firstname;
    }

    public void setFirstName(String firstname) {
        this.firstname = firstname;
    }

    public String getLastName() {
        return lastname;
    }

    public void setLastName(String lastname) {
        this.lastname = lastname;
    }

    
}
