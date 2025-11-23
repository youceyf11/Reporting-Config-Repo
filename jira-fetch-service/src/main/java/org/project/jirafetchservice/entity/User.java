package org.project.jirafetchservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "user")
public class User {
  @Id private UUID id;

  private String username;
  private String email;
  private String fullName;
}