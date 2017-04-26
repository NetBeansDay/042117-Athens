/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.web;

import com.example.model.Student;
import com.example.model.StudentDTO;
import com.example.repository.StudentRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 *
 * @author DiakogiannisA
 */
@RestController
@RequestMapping("/url")
public class NewRestController {
    
    @Autowired
    StudentRepository studentRepository;
    
    @RequestMapping(method = GET)
    public List<Student> list() {
        return studentRepository.findAll();
    }
    
    @RequestMapping(value = "/{id}", method = GET)
    public ResponseEntity<?> get(@PathVariable Long id) {
        Student student = studentRepository.findOne(id);
        if(student != null){
            return ResponseEntity.ok(student);
        }
        return ResponseEntity.notFound().build();
    }
    
    @RequestMapping(method = POST, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Student> post(@RequestBody StudentDTO input) {
        System.out.println(input.getFirstName());
        return ResponseEntity.ok(studentRepository.save(new Student(input.getFirstName(),input.getLastName())));
    }
    
    
}
