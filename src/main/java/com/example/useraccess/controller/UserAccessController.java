package com.example.useraccess.controller;
 
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
 
import com.example.useraccess.dto.PermissionCountResponse;
import com.example.useraccess.dto.PermissionRequest;
import com.example.useraccess.dto.PermissionResponse;
import com.example.useraccess.dto.RolePermissionRequest;
import com.example.useraccess.dto.SummaryResponse;
import com.example.useraccess.service.PermissionService;
 
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
 
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Access", description = "APIs for summary, permission count, effective permissions, and single-role permissions")
public class UserAccessController {
 
    private final PermissionService permissionService;
 
    public UserAccessController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
 
    @PostMapping("/summary")
    @Operation(
            summary = "Get user summary",
            description = "Returns summary information for the selected user without loading the full permission set.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Summary retrieved successfully",
                            content = @Content(schema = @Schema(implementation = SummaryResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    public SummaryResponse getSummary(@RequestBody PermissionRequest request) {
        return permissionService.getSummary(
                request.getEnv(),
                request.getUserId(),
                request.getRoles()
        );
    }
 
    @PostMapping("/permission-count")
    @Operation(
            summary = "Get effective permission count",
            description = "Returns distinct permission count for the selected user by combining role-based permissions and user-specific add-on permissions.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Permission count retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PermissionCountResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    public PermissionCountResponse getPermissionCount(@RequestBody PermissionRequest request) {
        int count = permissionService.getPermissionCount(
                request.getEnv(),
                request.getUserId(),
                request.getRoles()
        );
        return new PermissionCountResponse(request.getUserId(), request.getEnv(), count);
    }
 
    @PostMapping("/permissions")
    @Operation(
            summary = "Get effective user permissions",
            description = "Returns full distinct permission list for the selected user by combining permissions from all AD roles and user-specific add-on permissions.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Permissions retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PermissionResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    public PermissionResponse getPermissions(@RequestBody PermissionRequest request) {
        return permissionService.getPermissions(
                request.getEnv(),
                request.getUserId(),
                request.getRoles()
        );
    }
 
    @PostMapping("/role-permissions")
    @Operation(
            summary = "Get permissions for a single AD role",
            description = "Returns permissions derived from one AD group only. Supports the View perms action in the AD Groups tab.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Role permissions retrieved successfully",
                            content = @Content(schema = @Schema(implementation = PermissionResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    public PermissionResponse getRolePermissions(@RequestBody RolePermissionRequest request) {
        return permissionService.getPermissionsForSingleRole(
                request.getEnv(),
                request.getRoleName()
        );
    }
}