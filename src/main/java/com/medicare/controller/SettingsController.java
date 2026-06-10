package com.medicare.controller;

import com.medicare.model.SysUser;
import com.medicare.service.SysUserService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 系统设置控制器
 * 管理员 CRUD + 密码修改
 *
 * @author MediCare Team
 * @date 2026-06-10
 */
public class SettingsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final SysUserService sysUserService = new SysUserService();
    private final ObservableList<SysUser> adminList = FXCollections.observableArrayList();

    private SysUser editingAdmin = null;

    // ========== 管理员管理 Tab ==========
    @FXML private TextField txtAdminSearch;
    @FXML private TableView<SysUser> tableAdmins;
    @FXML private TableColumn<SysUser, Long> colAdminId;
    @FXML private TableColumn<SysUser, String> colAdminUsername, colAdminRealName, colAdminCreateTime;
    @FXML private TableColumn<SysUser, Integer> colAdminStatus;
    @FXML private Label lblAdminCount, lblAdminMessage;
    @FXML private TextField txtAdminUsername, txtAdminRealName;
    @FXML private PasswordField txtAdminPassword;
    @FXML private ComboBox<String> cmbAdminStatus;

    // ========== 修改密码 Tab ==========
    @FXML private PasswordField txtOldPassword, txtNewPassword, txtConfirmPassword;
    @FXML private Label lblPwdMessage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("系统设置界面初始化");
        initAdminTable();
        cmbAdminStatus.setItems(FXCollections.observableArrayList("停用", "启用"));
        cmbAdminStatus.getSelectionModel().select(1);
        loadAdminData();
    }

    // ============================================================
    // 管理员表格初始化
    // ============================================================

    private void initAdminTable() {
        colAdminId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAdminUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colAdminRealName.setCellValueFactory(new PropertyValueFactory<>("realName"));
        colAdminStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAdminStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : (s == 1 ? "启用" : "停用"));
            }
        });
        colAdminCreateTime.setCellValueFactory(new PropertyValueFactory<>("createTime"));
        tableAdmins.setItems(adminList);
        tableAdmins.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) loadAdminToForm(n);
        });
    }

    // ============================================================
    // 数据加载
    // ============================================================

    private void loadAdminData() {
        Task<List<SysUser>> task = new Task<>() {
            @Override protected List<SysUser> call() throws Exception { return sysUserService.listAllAdmin(); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            adminList.setAll(task.getValue());
            lblAdminCount.setText("共 " + adminList.size() + " 条");
        }));
        task.setOnFailed(e -> logger.error("加载管理员列表失败", task.getException()));
        new Thread(task).start();
    }

    // ============================================================
    // 管理员管理事件
    // ============================================================

    @FXML private void handleAdminSearch() {
        String kw = txtAdminSearch.getText();
        Task<List<SysUser>> task = new Task<>() {
            @Override protected List<SysUser> call() throws Exception { return sysUserService.searchAdmin(kw); }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            adminList.setAll(task.getValue());
            lblAdminCount.setText("共 " + adminList.size() + " 条");
        }));
        new Thread(task).start();
    }

    @FXML private void handleAdminNew() {
        editingAdmin = null;
        clearAdminForm();
    }

    @FXML private void handleAdminSave() {
        SysUser user = new SysUser();
        user.setUsername(txtAdminUsername.getText().trim());
        user.setPassword(txtAdminPassword.getText());
        user.setRealName(txtAdminRealName.getText().trim());
        user.setStatus(cmbAdminStatus.getSelectionModel().getSelectedIndex());

        if (user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
            showAdminError("用户名和密码不能为空"); return;
        }

        if (editingAdmin != null) {
            user.setId(editingAdmin.getId());
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception { sysUserService.updateAdmin(user); return null; }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadAdminData(); showAdminInfo("更新成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showAdminError(task.getException().getMessage())));
            new Thread(task).start();
        } else {
            Task<Long> task = new Task<>() {
                @Override protected Long call() throws Exception { return sysUserService.addAdmin(user); }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                loadAdminData(); clearAdminForm(); showAdminInfo("新增成功");
            }));
            task.setOnFailed(e -> Platform.runLater(() -> showAdminError(task.getException().getMessage())));
            new Thread(task).start();
        }
    }

    @FXML private void handleAdminClear() {
        clearAdminForm();
    }

    @FXML private void handleAdminDelete() {
        if (editingAdmin == null) { showAdminError("请先选择要删除的管理员"); return; }

        // 保护当前登录用户
        SysUser current = LoginController.getCurrentUser();
        if (current != null && current.getId().equals(editingAdmin.getId())) {
            showAdminError("不能删除当前登录的账号"); return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除管理员");
        alert.setHeaderText(null);
        alert.setContentText("确定删除管理员 " + editingAdmin.getUsername() + " 吗？");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override protected Void call() throws Exception {
                        sysUserService.deleteAdmin(editingAdmin.getId());
                        return null;
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    loadAdminData(); clearAdminForm(); showAdminInfo("删除成功");
                }));
                task.setOnFailed(e -> Platform.runLater(() -> showAdminError(task.getException().getMessage())));
                new Thread(task).start();
            }
        });
    }

    private void loadAdminToForm(SysUser user) {
        editingAdmin = user;
        txtAdminUsername.setText(user.getUsername());
        txtAdminPassword.setText(user.getPassword());
        txtAdminRealName.setText(user.getRealName());
        cmbAdminStatus.getSelectionModel().select(user.getStatus() != null && user.getStatus() == 1 ? 1 : 0);
    }

    private void clearAdminForm() {
        editingAdmin = null;
        txtAdminUsername.clear();
        txtAdminPassword.clear();
        txtAdminRealName.clear();
        cmbAdminStatus.getSelectionModel().select(1);
        lblAdminMessage.setText("");
        tableAdmins.getSelectionModel().clearSelection();
    }

    private void showAdminInfo(String msg) {
        lblAdminMessage.setStyle("-fx-text-fill: #27ae60;");
        lblAdminMessage.setText(msg);
    }

    private void showAdminError(String msg) {
        lblAdminMessage.setStyle("-fx-text-fill: #c0392b;");
        lblAdminMessage.setText(msg);
    }

    // ============================================================
    // 密码修改事件
    // ============================================================

    @FXML private void handleChangePassword() {
        SysUser current = LoginController.getCurrentUser();
        if (current == null) { showPwdError("未获取到当前登录用户"); return; }

        String oldPwd = txtOldPassword.getText();
        String newPwd = txtNewPassword.getText();
        String confirmPwd = txtConfirmPassword.getText();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                sysUserService.changePassword(current.getId(), oldPwd, newPwd, confirmPwd);
                return null;
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            txtOldPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
            showPwdInfo("密码修改成功，请使用新密码重新登录");
            logger.info("用户 {} 修改密码成功", current.getUsername());
        }));
        task.setOnFailed(e -> Platform.runLater(() -> showPwdError(task.getException().getMessage())));
        new Thread(task).start();
    }

    private void showPwdInfo(String msg) {
        lblPwdMessage.setStyle("-fx-text-fill: #27ae60;");
        lblPwdMessage.setText(msg);
    }

    private void showPwdError(String msg) {
        lblPwdMessage.setStyle("-fx-text-fill: #c0392b;");
        lblPwdMessage.setText(msg);
    }
}
