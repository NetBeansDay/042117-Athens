/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.repository;

import com.example.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 *
 * @author DiakogiannisA
 */
public interface StudentRepository extends JpaRepository<Student, Long> {
    
}
