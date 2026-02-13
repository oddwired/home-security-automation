package com.kshem.services.homesecurity.entities;

import com.kshem.services.homesecurity.utils.UuidProvider;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class FileDescriptor {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "FILE_ID", nullable = false)
  private String fileId = UuidProvider.createUuid().toString();

  @Column(name = "NAME", nullable = false)
  private String name;

  @Column(name = "EXTENSION")
  private String extension;

  @Column(name = "SIZE", nullable = false)
  private Long size;

  @Column(name = "CREATE_TS", nullable = false)
  private Date createTS;

  @Column(name = "DELETE_TS")
  private Date deleteTS;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    FileDescriptor that = (FileDescriptor) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
