# MediCare Docker 交付说明

## 你发给对方时需要包含的内容

- 整个 `MediCare` 项目目录
- 安装好的 Docker Desktop 环境

## 对方启动步骤

在项目根目录执行：

```bash
docker compose up --build -d
```

Windows 也可以直接双击：

- `docker-up.bat`

## 启动后的访问地址

- 前端：http://localhost:5173
- 后端：http://localhost:8080
- MySQL：localhost:3307

## 默认账号

- 用户名：`admin`
- 密码：`12345`

## 数据库说明

Docker 会自动完成这些动作：

- 启动 MySQL 8.0
- 创建 `medicare` 数据库
- 自动导入 `sql/medicare.sql`

数据库容器账号：

- 数据库名：`medicare`
- 用户名：`medicare`
- 密码：`medicare123`
- Root 密码：`root123456`

## 停止服务

```bash
docker compose down
```

Windows 也可以直接双击：

- `docker-down.bat`

## 彻底清空数据库数据

```bash
docker compose down -v
```

下次重新启动时会重新导入初始数据。
