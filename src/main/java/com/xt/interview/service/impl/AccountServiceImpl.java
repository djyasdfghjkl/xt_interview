package com.xt.interview.service.impl;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.PageResult;
import com.xt.interview.entity.Account;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.AccountRepository;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.service.AccountService;
import com.xt.interview.util.MD5Util;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    private static final String DEFAULT_PASSWORD = "123456";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public AccountDTO createAccount(AccountDTO dto) {
        validateBasic(dto);
        if (accountRepository.existsByUsernameAndDeleted(dto.getUsername().trim(), NOT_DELETED)) {
            throw new RuntimeException("用户名已存在");
        }
        validateDepartment(dto.getDepartmentId());

        Account account = new Account();
        account.setUsername(dto.getUsername().trim());
        account.setName(dto.getName().trim());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        String password = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? DEFAULT_PASSWORD : dto.getPassword();
        account.setPassword(MD5Util.encode(password));
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        account.setIsSuper(dto.getIsSuper());
        account.setDepartmentId(dto.getDepartmentId());
        account.setDeleted(NOT_DELETED);
        account = accountRepository.save(account);
        return convertToDTO(account);
    }

    @Override
    public AccountDTO updateAccount(Long id, AccountDTO dto) {
        validateBasic(dto);

        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (!account.getUsername().equals(dto.getUsername().trim()) &&
            accountRepository.existsByUsernameAndDeleted(dto.getUsername().trim(), NOT_DELETED)) {
            throw new RuntimeException("用户名已存在");
        }
        validateDepartment(dto.getDepartmentId());

        account.setUsername(dto.getUsername().trim());
        account.setName(dto.getName().trim());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            account.setPassword(MD5Util.encode(dto.getPassword()));
        }
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        account.setIsSuper(dto.getIsSuper());
        account.setDepartmentId(dto.getDepartmentId());
        account = accountRepository.save(account);
        return convertToDTO(account);
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        account.setDeleted(DELETED);
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        return convertToDTO(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        return accountRepository.findByDeleted(NOT_DELETED).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AccountDTO> getAccountsPage(String username, Integer status, Integer roleLevel,
                                                   int page, int size) {
        Specification<Account> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), NOT_DELETED));

            if (StringUtils.hasText(username)) {
                String keyword = "%" + username.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.<String>get("username")), keyword));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (roleLevel != null) {
                predicates.add(cb.equal(root.get("roleLevel"), roleLevel));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "id"));
        Page<Account> accountPage = accountRepository.findAll(spec, pageRequest);

        List<AccountDTO> list = accountPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new PageResult<>(
                list,
                accountPage.getTotalElements(),
                accountPage.getTotalPages(),
                accountPage.getNumber(),
                accountPage.getSize()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO login(String username, String password) {
        Account account = accountRepository.findByUsernameAndPasswordAndDeleted(
                username, MD5Util.encode(password), NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        if (account.getStatus() != 1) {
            throw new RuntimeException("账号已禁用");
        }
        return convertToDTO(account);
    }

    private void validateBasic(AccountDTO dto) {
        if (dto == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new RuntimeException("用户名不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new RuntimeException("姓名不能为空");
        }
    }

    private void validateDepartment(Long departmentId) {
        if (departmentId == null) {
            return;
        }
        Department department = departmentRepository.findByIdAndDeleted(departmentId, NOT_DELETED).orElse(null);
        if (department == null) {
            throw new RuntimeException("所属部门不存在");
        }
    }

    private AccountDTO convertToDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setName(account.getName());
        dto.setGender(account.getGender());
        dto.setPhone(account.getPhone());
        dto.setRoleLevel(account.getRoleLevel());
        dto.setStatus(account.getStatus());
        dto.setIsSuper(account.getIsSuper());
        dto.setDepartmentId(account.getDepartmentId());

        if (account.getDepartmentId() != null) {
            Department department = departmentRepository.findByIdAndDeleted(account.getDepartmentId(), NOT_DELETED)
                    .orElse(null);
            dto.setDepartmentName(department == null ? null : department.getName());
        }

        dto.setCreateTime(account.getCreateTime());
        dto.setUpdateTime(account.getUpdateTime());
        return dto;
    }
}
