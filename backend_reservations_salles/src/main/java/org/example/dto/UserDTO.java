package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String department;
    private List<String> roles;

    public UserDTO(Long id, String name, String email, String department) {
        this(id, name, email, department, List.of());
    }
}
