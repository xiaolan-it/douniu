# H5斗牛（牛牛）游戏

一个完整的H5斗牛游戏项目，包含前后端完整实现。

## 技术栈

### 后端
- Spring Boot 3.2.0
- Spring WebSocket (STOMP)
- Sa-Token (认证授权)
- MyBatis-Plus
- MySQL 8.0
- Redis
- Lombok

### 前端
- Vue 3
- Vite
- Vue Router
- Pinia
- TailwindCSS
- StompJS (WebSocket客户端)

## 项目结构

```
douniu/
├── douniu-backend/          # 后端项目
│   ├── src/main/java/com/douniu/
│   │   ├── config/          # 配置类
│   │   ├── controller/       # HTTP控制器
│   │   ├── websocket/        # WebSocket处理器
│   │   ├── service/          # 业务逻辑层
│   │   ├── mapper/           # MyBatis Mapper
│   │   ├── entity/           # 实体类
│   │   ├── dto/              # 数据传输对象
│   │   ├── enums/            # 枚举类
│   │   └── utils/            # 工具类
│   └── src/main/resources/
│       ├── application.yml
│       └── db/schema.sql
└── douniu-frontend/          # 前端项目
    ├── src/
    │   ├── views/            # 页面组件
    │   ├── components/       # 组件
    │   ├── stores/           # Pinia状态管理
    │   ├── router/           # 路由配置
    │   └── utils/            # 工具函数
    └── package.json
```

## 环境要求

- JDK 17+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+

## 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE douniu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行SQL脚本：
```bash
mysql -u root -p douniu < douniu-backend/src/main/resources/db/schema.sql
```

3. 修改 `application.yml` 中的数据库配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/douniu?...
    username: root
    password: root
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: 123456
```

## 后端启动

1. 进入后端目录：
```bash
cd douniu-backend
```

2. 使用Maven构建：
```bash
mvn clean install
```

3. 运行应用：
```bash
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

## 前端启动

1. 进入前端目录：
```bash
cd douniu-frontend
```

2. 安装依赖：
```bash
npm install
```

3. 启动开发服务器：
```bash
npm run dev
```

前端服务将在 `http://localhost:3000` 启动。

## 核心功能

### 1. 用户系统
- 手机号+密码注册登录
- Sa-Token认证
- 用户积分管理（全局积分）

### 2. 房间系统
- 创建房间（UUID房间号，去掉横杆）
- 加入房间（通过房间号或URL）
- 最多10人
- 房间管理员设置

### 3. 游戏功能
- 管理员指定局数（默认20局）
- 投注额选择（10/20/30/40/50元）
- 牌型规则配置（可勾选启用的牌型）
- 庄家指定/轮换
- 发牌动画（从中心向各座位发牌）
- 倒计时（投注15秒，开牌10秒）
- 牌型计算（五小牛、炸弹牛、五花牛、顺子牛、牛牛、牛9-1、无牛）
- 积分结算
- 积分变化动画（±积分显示，3秒消失）

### 4. 积分系统
- 用户表积分（balance）：全局积分，累加/减
- 房间内积分（total_score）：房间开局时重置为0，之后每局累加/减，直到房间结束

### 5. 对局记录
- 记录每局牌型、牌大小、输赢积分、时间
- 对局汇总查询

## API接口

### 认证接口
- `POST /api/auth/register` - 注册
- `POST /api/auth/login` - 登录
- `GET /api/auth/me` - 获取当前用户
- `POST /api/auth/logout` - 退出登录

### 房间接口
- `POST /api/room/create` - 创建房间
- `GET /api/room/code/{roomCode}` - 根据房间号获取房间
- `GET /api/room/{roomId}/players` - 获取房间玩家

### 对局记录接口
- `GET /api/game-record/user` - 获取用户对局记录
- `GET /api/game-record/room/{roomId}` - 获取房间对局记录
- `GET /api/game-record/{gameRecordId}/details` - 获取对局详情

## WebSocket消息

### 客户端发送
- `/app/room/join` - 加入房间
- `/app/room/leave` - 离开房间
- `/app/room/setAdmin` - 设置管理员
- `/app/game/start` - 开始游戏
- `/app/game/bet` - 投注
- `/app/game/deal` - 发牌
- `/app/game/reveal` - 开牌
- `/app/game/settle` - 结算
- `/app/game/finish` - 提前结算

### 服务端推送
- `/topic/room/{roomId}/update` - 房间更新
- `/topic/room/{roomId}/game/start` - 游戏开始
- `/topic/room/{roomId}/game/bet` - 投注通知
- `/topic/room/{roomId}/game/deal` - 发牌通知
- `/topic/room/{roomId}/game/reveal` - 开牌通知
- `/topic/room/{roomId}/game/settle` - 结算通知
- `/topic/room/{roomId}/game/finish` - 游戏结束

## 牌型说明

| 牌型 | 赔率 | 说明 |
|------|------|------|
| 五小牛 | ×7 | 5张牌都小于5，且总和≤10 |
| 炸弹牛 | ×6 | 4张相同点数的牌 |
| 五花牛 | ×5 | 5张都是J、Q、K |
| 顺子 | ×4 | 5张连续 |
| 牛牛 | ×3 | 前3张组成10的倍数，后2张和也是10的倍数 |
| 牛9/牛8 | ×2 | 有牛，且剩余2张和为9或8 |
| 牛7~牛1 | ×1 | 有牛，且剩余2张和为7~1 |
| 无牛 | ×1 | 无法组成10的倍数 |

## 注意事项

1. 确保MySQL和Redis服务已启动
2. 修改配置文件中的数据库和Redis连接信息
3. 前端WebSocket地址需要根据实际后端地址修改
4. Cards.png图片资源需要放在 `douniu-frontend/src/assets/` 目录下

## 开发说明

- 后端使用Spring Boot 3，需要JDK 17+
- 前端使用Vue 3 Composition API
- WebSocket使用STOMP协议
- 所有数据交互使用JSON格式
- 实时通信使用WebSocket，查询操作使用HTTP

## 许可证

MIT License

