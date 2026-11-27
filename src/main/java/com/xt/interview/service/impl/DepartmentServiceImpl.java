package com.xt.interview.service.impl;

import com.xt.interview.dto.DepartmentDTO;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findByDeleted(NOT_DELETED).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        return convertToDTO(department);
    }

    @Override
    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        validate(dto);

        if (departmentRepository.existsByCodeAndDeleted(dto.getCode().trim(), NOT_DELETED)) {
            throw new RuntimeException("部门编码已存在");
        }

        Department department = new Department();
        applyFields(department, dto);
        department.setDeleted(NOT_DELETED);
        department = departmentRepository.save(department);
        return convertToDTO(department);
    }

    @Override
    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) {
        validate(dto);

        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        if (departmentRepository.existsByCodeAndDeletedAndIdNot(dto.getCode().trim(), NOT_DELETED, id)) {
            throw new RuntimeException("部门编码已存在");
        }

        applyFields(department, dto);
        department = departmentRepository.save(department);
        return convertToDTO(department);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        department.setDeleted(DELETED);
        departmentRepository.save(department);
    }

    private void validate(DepartmentDTO dto) {
        if (dto == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("部门名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCode())) {
            throw new RuntimeException("部门编码不能为空");
        }
    }

    private void applyFields(Department department, DepartmentDTO dto) {
        department.setName(dto.getName().trim());
        department.setCode(dto.getCode().trim());
        department.setDescription(dto.getDescription());
        department.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setCode(department.getCode());
        dto.setDescription(department.getDescription());
        dto.setStatus(department.getStatus());
        dto.setDeleted(department.getDeleted());
        dto.setCreateTime(department.getCreateTime());
        dto.setUpdateTime(department.getUpdateTime());
        return dto;
    }
}
