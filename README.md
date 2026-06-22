<div align="center">

# 🧠 SZYX AI

**一款支持多种云端 AI 模型的角色扮演聊天应用**

![Android](https://img.shields.io/badge/Android-8.0%2B-green?style=flat-square&logo=android)
![Java](https://img.shields.io/badge/Java-11-orange?style=flat-square&logo=openjdk)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)
![Version](https://img.shields.io/badge/Version-1.0.0-purple?style=flat-square)
![Stars](https://img.shields.io/github/stars/2919671430/szyx?style=flat-square)

### 📥 直接下载

[![Download APK](https://img.shields.io/badge/📥_下载_APP-SZYX--AI--v1.0.0.apk-blue?style=for-the-badge&logo=android)](https://github.com/2919671430/szyx/raw/master/SZYX-AI-v1.0.0.apk)

> 💡 直接下载安装即可使用，无需编译

[English](#english) | 中文

</div>

---

## ✨ 核心特性

| 功能 | 说明 |
|:---:|:---|
| 🤖 **多模型支持** | 支持小米 mimo、DeepSeek、自定义 API 等多种云端 AI 模型 |
| 🎭 **角色卡系统** | 自由创建角色人设、世界观、输出风格，兼容 SillyTavern 格式 |
| 🧠 **双记忆体系** | 短期记忆（最近 N 轮）+ 长期记忆（自动提取关键剧情） |
| 📚 **世界书知识库** | 关键词触发的世界观设定注入，宏大世界观也能高效使用 |
| 📊 **养成数值系统** | AI 自动解析数值变化，支持好感度、RPG 属性等 |
| 🎨 **模板一键套用** | 5 套预设模板，新手秒出作品 |

## 🚀 快速开始

### 环境要求

- Android 8.0 (API 26) 及以上
- arm64-v8a 架构设备
- 建议 4GB+ 运行内存

### 安装方式

**方式一：直接下载（推荐）**

点击上方下载按钮，或直接下载 [SZYX-AI-v1.0.0.apk](https://github.com/2919671430/szyx/raw/master/SZYX-AI-v1.0.0.apk)，安装后在设置中配置 API Key 即可使用。

**方式二：从源码构建**

```bash
# 克隆项目
git clone https://github.com/2919671430/szyx.git

# 进入项目目录
cd szyx

# 构建 Debug 版本
./gradlew assembleDebug
```

## 📱 功能详解

### 🤖 多模型支持

支持多种云端 AI 服务，随时切换不丢失对话：

| 服务 | 模型 | 特点 |
|:---:|:---:|:---|
| **小米 mimo** | mimo-v2.5 | 低延迟、高质量回复 |
| **DeepSeek** | DeepSeek Chat | 强大的中文理解能力 |
| **自定义 API** | 兼容 OpenAI 格式 | 灵活接入各种服务 |

### 🧠 智能记忆系统

```
┌─────────────────────────────────────────────────────────┐
│                     Prompt 分层架构                       │
├─────────────────────────────────────────────────────────┤
│  1. 系统骨架    ─── 内置规则，永不截断                     │
│  2. 用户设定    ─── 角色人设 + 世界观，永不截断            │
│  3. 长期记忆    ─── 关键剧情/抉择/关系，上限 1500 Token    │
│  4. 世界书      ─── 关键词触发设定，上限 2000 Token        │
│  5. 养成数值    ─── 当前数值状态                          │
│  6. 短期记忆    ─── 最近 N 轮对话，从最旧开始截断          │
│  7. 用户消息    ─── 最新输入，永不截断                     │
└─────────────────────────────────────────────────────────┘
```

### 📊 养成数值系统

```java
// AI 自动输出数值变化标记
"你送给她一朵花，她开心地笑了 [好感+5]"

// 系统自动解析并更新
好感: 75/100 | 修为: 30/100 | 金币: 500
```

## 🛠️ 技术栈

| 类别 | 技术 |
|:---:|:---|
| **语言** | Java 11 |
| **架构** | MVVM (ViewModel + LiveData) |
| **数据库** | Room (SQLite) |
| **网络** | OkHttp + SSE (流式传输) |
| **UI** | Material Design 3 |
| **异步** | WorkManager |
| **图片** | Glide |
| **构建** | Gradle 8.x |

## 📁 项目结构

```
szyx/
├── app/
│   ├── src/main/java/com/szyx/ai/
│   │   ├── engine/              # 推理引擎
│   │   │   ├── api/             # 云端 API 实现
│   │   │   │   ├── XiaomiApiEngine.java
│   │   │   │   ├── DeepSeekApiEngine.java
│   │   │   │   └── CustomApiEngine.java
│   │   │   ├── memory/          # 记忆管理
│   │   │   ├── prompt/          # Prompt 构建
│   │   │   └── numerical/       # 数值系统
│   │   ├── data/                # 数据层
│   │   │   ├── db/              # Room 数据库
│   │   │   └── repository/      # 数据仓库
│   │   ├── ui/                  # 界面层
│   │   │   ├── chat/            # 聊天界面
│   │   │   ├── character/       # 角色管理
│   │   │   └── settings/        # 设置页面
│   │   └── viewmodel/           # ViewModel
│   └── schemas/                 # 数据库 Schema
└── 使用手册.md                   # 详细使用文档
```

## 🤝 参与贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 开源协议

本项目基于 [MIT License](LICENSE) 开源。

## 🙏 致谢

- [Material Design 3](https://m3.material.io/) - UI 设计规范
- [SillyTavern](https://github.com/SillyTavern/SillyTavern) - 角色卡格式参考

---

<div align="center">

**如果觉得有用，请给个 ⭐ Star 支持一下！**

</div>
