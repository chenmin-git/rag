package com.example.campusrag.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.campusrag.common.ApiResponse;
import com.example.campusrag.common.BusinessException;
import com.example.campusrag.domain.KnowledgeChunk;
import com.example.campusrag.domain.KnowledgeDocument;
import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.service.AuditService;
import com.example.campusrag.service.AuthService;
import com.example.campusrag.service.DocumentService;
import com.example.campusrag.service.PermissionEvaluator;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final DocumentService documentService;
    private final AuthService authService;
    private final AuditService auditService;
    private final PermissionEvaluator permissionEvaluator;

    public DocumentController(
            DocumentService documentService,
            AuthService authService,
            AuditService auditService,
            PermissionEvaluator permissionEvaluator) {
        this.documentService = documentService;
        this.authService = authService;
        this.auditService = auditService;
        this.permissionEvaluator = permissionEvaluator;
    }

    @GetMapping
    public ApiResponse<List<KnowledgeDocument>> list(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        UserAccount user = authService.requireCurrentUser(userId);
        return ApiResponse.ok(documentService.listDocuments().stream()
                .filter(document -> permissionEvaluator.canAccess(document.getVisibility(), document.getDepartment(), user))
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDocument> detail(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        UserAccount user = authService.requireCurrentUser(userId);
        KnowledgeDocument document = documentService.getDocument(id);
        ensureCanRead(document, user);
        return ApiResponse.ok(document);
    }

    @PostMapping
    public ApiResponse<KnowledgeDocument> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "教务处") String department,
            @RequestParam(defaultValue = "PUBLIC") String visibility,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            HttpServletRequest request) {
        UserAccount user = authService.requireAnyRole(userId, "SYSTEM_ADMIN", "DEPARTMENT_ADMIN");
        String targetDepartment = normalizeDepartment(department, user);
        ensureCanManageDepartment(user, targetDepartment);
        KnowledgeDocument document = documentService.upload(file, targetDepartment, visibility, user);
        auditService.log(user, "DOCUMENT_UPLOAD", document.getOriginalName(), "上传并向量化入库", request.getRemoteAddr(), true);
        return ApiResponse.ok(document);
    }

    @PostMapping("/text")
    public ApiResponse<KnowledgeDocument> createText(
            @RequestBody TextDocumentRequest textRequest,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            HttpServletRequest request) {
        UserAccount user = authService.requireAnyRole(userId, "SYSTEM_ADMIN", "DEPARTMENT_ADMIN");
        String targetDepartment = normalizeDepartment(textRequest.department(), user);
        ensureCanManageDepartment(user, targetDepartment);
        KnowledgeDocument document = documentService.createTextDocument(
                textRequest.title(),
                textRequest.content(),
                targetDepartment,
                textRequest.visibility(),
                user);
        auditService.log(user, "DOCUMENT_TEXT_CREATE", document.getOriginalName(), "手动录入资料并向量化", request.getRemoteAddr(), true);
        return ApiResponse.ok(document);
    }

    @GetMapping("/{id}/chunks")
    public ApiResponse<List<KnowledgeChunk>> chunks(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        UserAccount user = authService.requireCurrentUser(userId);
        ensureCanRead(documentService.getDocument(id), user);
        return ApiResponse.ok(documentService.chunks(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeDocument> updateMetadata(
            @PathVariable Long id,
            @RequestBody DocumentMetadataRequest metadataRequest,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            HttpServletRequest request) {
        UserAccount user = authService.requireAnyRole(userId, "SYSTEM_ADMIN", "DEPARTMENT_ADMIN");
        KnowledgeDocument before = documentService.getDocument(id);
        ensureCanManageDocument(user, before);
        String targetDepartment = metadataRequest.department() == null || metadataRequest.department().isBlank()
                ? before.getDepartment()
                : metadataRequest.department().trim();
        ensureCanManageDepartment(user, targetDepartment);
        KnowledgeDocument document = documentService.updateMetadata(id, targetDepartment, metadataRequest.visibility());
        auditService.log(user, "DOCUMENT_UPDATE", document.getOriginalName(), "更新部门或可见范围并重建索引", request.getRemoteAddr(), true);
        return ApiResponse.ok(document);
    }

    @PostMapping("/{id}/rebuild")
    public ApiResponse<KnowledgeDocument> rebuild(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            HttpServletRequest request) {
        UserAccount user = authService.requireAnyRole(userId, "SYSTEM_ADMIN", "DEPARTMENT_ADMIN");
        ensureCanManageDocument(user, documentService.getDocument(id));
        KnowledgeDocument document = documentService.rebuild(id);
        auditService.log(user, "DOCUMENT_REBUILD", document.getOriginalName(), "重新解析、切片和向量化", request.getRemoteAddr(), true);
        return ApiResponse.ok(document);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            HttpServletRequest request) {
        UserAccount user = authService.requireAnyRole(userId, "SYSTEM_ADMIN", "DEPARTMENT_ADMIN");
        ensureCanManageDocument(user, documentService.getDocument(id));
        documentService.delete(id);
        auditService.log(user, "DOCUMENT_DELETE", String.valueOf(id), "删除文档及向量切片", request.getRemoteAddr(), true);
        return ApiResponse.ok(null);
    }

    public record TextDocumentRequest(String title, String content, String department, String visibility) {
    }

    public record DocumentMetadataRequest(String department, String visibility) {
    }

    private void ensureCanRead(KnowledgeDocument document, UserAccount user) {
        if (!permissionEvaluator.canAccess(document.getVisibility(), document.getDepartment(), user)) {
            throw new BusinessException("无权访问该文档");
        }
    }

    private void ensureCanManageDocument(UserAccount user, KnowledgeDocument document) {
        ensureCanManageDepartment(user, document.getDepartment());
    }

    private void ensureCanManageDepartment(UserAccount user, String department) {
        if ("SYSTEM_ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }
        if ("DEPARTMENT_ADMIN".equalsIgnoreCase(user.getRole())
                && department != null
                && department.equalsIgnoreCase(user.getDepartment())) {
            return;
        }
        throw new BusinessException("只能维护本部门知识库");
    }

    private String normalizeDepartment(String department, UserAccount user) {
        if (department == null || department.isBlank()) {
            return user.getDepartment();
        }
        return department.trim();
    }
}
