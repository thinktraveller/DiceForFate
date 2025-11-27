## 文档目标
- 生成 README.md，系统性说明项目中每个文件/目录的作用，便于归档与后续维护。
- 覆盖根目录、Gradle 配置、app 模块（源码、资源、清单、测试）、dist/ 与 archive/ 等。

## 文档结构
- 项目概览：简述“Dice for Fate”的功能与离线特性。
- 根目录文件说明：
  - `settings.gradle`：Gradle 子模块定义
  - `build.gradle`：项目级构建脚本与仓库配置
  - `gradle.properties`：全局构建/AndroidX选项
  - `gradlew` / `gradlew.bat`：Gradle Wrapper 启动脚本
  - `gradle/wrapper/gradle-wrapper.jar` / `gradle-wrapper.properties`：Wrapper 配置与二进制
- 交付与归档：
  - `dist/DiceForFate-v1.4.0-debug.apk`：最新调试版APK
  - `dist/checksums.txt`：APK 的 SHA-256 校验值
  - `archive/DiceForFate-v1.4.0-debug.zip`：发行包压缩文件（APK+校验）
  - `archive/DiceForFate-source-v1.4.0.zip`：源码归档压缩文件
- app 模块文件说明：
  - `app/build.gradle`：应用模块构建配置、依赖与编译选项
  - `app/proguard-rules.pro`：混淆规则（当前留空/示例）
  - `app/src/main/AndroidManifest.xml`：应用清单、入口Activity与权限
  - 源码（`app/src/main/java/com/example/dice/`）：
    - `DiceRoller.kt`：SecureRandom 掷骰核心逻辑
    - `HistoryStore.kt`：会话级历史记录共享 LiveData 仓库
    - `MainViewModel.kt`：触发掷骰、写入历史并输出当前结果
    - `MainActivity.kt`：主界面交互（固定骰型、MDN对话框、字号调节、结果居中显示）
    - `HistoryActivity.kt`：历史记录列表界面与清空功能
    - `ResultFormatter.kt`：结果文本格式化（m=1单值、m≥2两行）
    - `model/Dice.kt`：骰子模型（m、n）
    - `model/RollResult.kt`：掷骰结果数据结构
    - `ui/ResultAdapter.kt`：历史列表适配器（每次记录的骰型、结果、总和、时间）
  - 资源（`app/src/main/res/`）：
    - `layout/activity_main.xml`：主界面布局（按钮网格、结果显示区、字号调节）
    - `layout/activity_history.xml`：历史界面布局（RecyclerView+清空按钮）
    - `layout/item_roll_result.xml`：历史记录项布局
    - `values/strings.xml`：字符串资源（名称、文案、提示）
    - `values/colors.xml`：颜色配置
    - `values/themes.xml`：主题样式（MaterialComponents NoActionBar）
    - `values/ic_launcher_background.xml`、`drawable/ic_launcher_foreground.xml`、`mipmap-anydpi-v26/*`：应用图标资源
  - 测试（`app/src/test/java/com/example/dice/`）：
    - `DiceRollerTest.kt`：掷骰结果范围与分布的基础测试
    - `ResultFormatterTest.kt`：m=1与m≥2显示规则测试
- 工具目录：
  - `tools/gradle-8.1.1/`：本地临时Gradle安装残留（归档可忽略，不参与构建）

## 维护与构建指南（简要）
- 构建：在已安装 Android SDK 的环境执行 `./gradlew assembleDebug`。
- 安装：使用 APK 或 `adb install -r` 安装；APP 启动即用，完全离线。
- 功能要点：固定骰型、MDN自定义、居中显示、结果与总和独立字号调节、历史记录二级界面。

## 交付方式
- 我将新建 README.md 于项目根目录，写入上述内容（按目录分节、逐文件说明）。
- 可选：附上命令示例、版本历史与后续增强建议（若你需要可添加）。

请确认，我将生成 README.md 并保存到项目根目录。