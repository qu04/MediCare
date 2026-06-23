# Azure VM 部署说明

这套项目现在已经支持用 `git + docker compose` 的方式部署到 Azure Linux VM。

## 推荐方式

推荐使用 Ubuntu 22.04 或 24.04 的 Azure Linux VM，把前端直接映射到 `80` 端口，对外只开放：

- `22`：SSH
- `80`：网站访问

数据库和后端接口不建议直接暴露到公网。

## 代码仓库准备

当前这个本地仓库还没有配置远程仓库，所以你需要先把代码推到 GitHub 或 Gitee，例如：

```bash
git remote add origin <你的仓库地址>
git branch -M main
git push -u origin main
```

## 虚拟机初始化

进入 Azure VM 后，先安装 Git、Docker Engine、Docker Compose Plugin。

```bash
sudo apt-get update
sudo apt-get install -y git ca-certificates curl
```

Docker 官方推荐在 Ubuntu 上通过 Docker 仓库安装 `docker-ce`、`docker-ce-cli`、`containerd.io`、`docker-buildx-plugin`、`docker-compose-plugin`。

安装完成后验证：

```bash
docker compose version
sudo systemctl status docker
```

## 首次部署

```bash
git clone <你的仓库地址>
cd MediCare
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

部署完成后，站点默认通过 VM 的 `80` 端口访问：

```text
http://<你的Azure公网IP>/
```

## 后续更新

以后更新代码时，在虚拟机项目目录执行：

```bash
bash scripts/deploy-prod.sh
```

停止服务：

```bash
bash scripts/stop-prod.sh
```

## Azure 侧要做的设置

1. 暂停或删除你现在虚拟机上占用 `80` 端口的旧网页服务。
2. 在 Azure VM 对应的 NSG 入站规则里放行：
   - `22`
   - `80`
3. 如果你后面要接域名和 HTTPS，再额外加 `443`。

## 默认账号

- 管理员：`admin`
- 密码：`12345`

## 说明

- 生产覆盖文件是 [docker-compose.prod.yml](./docker-compose.prod.yml)
- 它会把前端改为直接监听 `80`
- 它不会把 MySQL 和后端端口暴露到公网
