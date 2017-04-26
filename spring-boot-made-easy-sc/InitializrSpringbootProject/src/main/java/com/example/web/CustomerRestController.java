/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.web;

import com.example.model.Customer;
import com.example.model.CustomerDTO;
import com.example.repository.CustomerRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 *
 * @author DiakogiannisA
 */
@RestController
@RequestMapping("/api")
public class CustomerRestController {
   
    @Autowired
    CustomerRepository customerRepository;
    
    @RequestMapping(method = GET)
    public List<Customer> list() {
        return customerRepository.findAll();
    }
    
    @RequestMapping(value = "/{id}", method = GET)
    public ResponseEntity<?> get(@PathVariable Long id) {
        Customer customer = customerRepository.findOne(id);
        if(customer != null){
            return ResponseEntity.ok(customer);
        }
        return ResponseEntity.notFound().build();
    }
    
   
    
    @RequestMapping(method = POST, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> post(@RequestBody CustomerDTO input) {
        return ResponseEntity.ok(customerRepository.save(new Customer(input.getFirstName(),input.getLastName())));
    }
    
   
    
}
