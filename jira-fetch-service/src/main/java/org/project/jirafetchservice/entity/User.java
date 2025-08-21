package org.project.jirafetchservice.entity;

import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("user")
public class User {
  @Id private UUID id;

  private String username;
  private String email;
  private String fullName;
}
