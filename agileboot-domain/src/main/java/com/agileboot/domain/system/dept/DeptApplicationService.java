package com.agileboot.domain.system.dept;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.StrUtil;
import com.agileboot.domain.common.dto.TreeSelectedDTO;
import com.agileboot.domain.system.dept.command.AddDeptCommand;
import com.agileboot.domain.system.dept.command.UpdateDeptCommand;
import com.agileboot.domain.system.dept.dto.DeptDTO;
import com.agileboot.domain.system.dept.model.DeptModel;
import com.agileboot.domain.system.dept.model.DeptModelFactory;
import com.agileboot.domain.system.dept.query.DeptQuery;
import com.agileboot.infrastructure.web.domain.login.LoginUser;
import com.agileboot.orm.entity.SysDeptEntity;
import com.agileboot.orm.entity.SysRoleEntity;
import com.agileboot.orm.service.ISysDeptService;
import com.agileboot.orm.service.ISysRoleService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 部门服务
 * @author valarchie
 */
@Service
public class DeptApplicationService {

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private ISysRoleService roleService;


    public List<DeptDTO> getDeptList(DeptQuery query) {
        List<SysDeptEntity> list = deptService.list(query.toQueryWrapper());
        return list.stream().map(DeptDTO::new).collect(Collectors.toList());
    }

    public DeptDTO getDeptInfo(Long id) {
        SysDeptEntity byId = deptService.getById(id);
        return new DeptDTO(byId);
    }

    public List<Tree<Long>> getDeptTree() {
        List<SysDeptEntity> list = deptService.list();

        return TreeUtil.build(list, 0L, (dept, tree) -> {
            tree.setId(dept.getDeptId());
            tree.setParentId(dept.getParentId());
            tree.putExtra("label", dept.getDeptName());
        });
    }

    public TreeSelectedDTO getDeptTreeForRole(Long roleId) {
        List<Long> checkedKeys = new ArrayList<>();
        SysRoleEntity role = roleService.getById(roleId);
        if (role != null && StrUtil.isNotEmpty(role.getDeptIdSet())) {
            checkedKeys = StrUtil.split(role.getDeptIdSet(), ",")
                .stream().map(Long::new).collect(Collectors.toList());
        }

        TreeSelectedDTO selectedDTO = new TreeSelectedDTO();
        selectedDTO.setDepts(getDeptTree());
        selectedDTO.setCheckedKeys(checkedKeys);

        return selectedDTO;
    }


    @Transactional(rollbackFor = Exception.class)
    public void addDept(AddDeptCommand addCommand, LoginUser loginUser) {
        DeptModel deptModel = DeptModelFactory.loadFromAddCommand(addCommand, new DeptModel());

        deptModel.checkDeptNameUnique(deptService);
        deptModel.generateAncestors(deptService);
        deptModel.logCreator(loginUser);

        deptModel.insert();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDept(UpdateDeptCommand updateCommand, LoginUser loginUser) {
        DeptModel deptModel = DeptModelFactory.loadFromUpdateCommand(updateCommand, deptService);

        deptModel.checkDeptNameUnique(deptService);
        deptModel.checkParentIdConflict();
        deptModel.checkStatusAllowChange(deptService);
        deptModel.generateAncestors(deptService);
        deptModel.logUpdater(loginUser);

        deptModel.updateById();
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeDept(Long deptId) {
        DeptModel deptModel = DeptModelFactory.loadFromDb(deptId, deptService);

        deptModel.checkHasChildDept(deptService);
        deptModel.checkDeptAssignedToUsers(deptService);

        deptService.removeById(deptId);
    }



}
