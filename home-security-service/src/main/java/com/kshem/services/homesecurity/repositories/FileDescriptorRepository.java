package com.kshem.services.homesecurity.repositories;

import com.kshem.services.homesecurity.entities.FileDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.Date;
import java.util.List;

@Repository
public interface FileDescriptorRepository extends JpaRepository<FileDescriptor, Long> {
    FileDescriptor findFileDescriptorByFileId(String fileId);
    Page<FileDescriptor> findAllByOrderByIdDesc(Pageable pageable);

    List<FileDescriptor> findFileDescriptorsByCreateTSBefore(Date date);
}
